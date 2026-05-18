package com.petfinder.chat.config;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.petfinder.chat.service.FirebaseAuthService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final FirebaseAuthService firebaseAuthService;

    public StompAuthChannelInterceptor(FirebaseAuthService firebaseAuthService) {
        this.firebaseAuthService = firebaseAuthService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String raw = accessor.getFirstNativeHeader("Authorization");
        if (raw == null || !raw.startsWith(BEARER_PREFIX)) {
            throw new SecurityException("Missing or invalid Authorization header on STOMP CONNECT");
        }

        String token = raw.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new SecurityException("Empty bearer token on STOMP CONNECT");
        }

        try {
            FirebaseToken firebaseToken = firebaseAuthService.verifyToken(token);
            accessor.setUser(new FirebaseUserAuthentication(firebaseToken));
        } catch (FirebaseAuthException e) {
            throw new SecurityException("Invalid Firebase token on STOMP CONNECT", e);
        }

        return message;
    }

    private static class FirebaseUserAuthentication extends AbstractAuthenticationToken {
        private final String uid;

        FirebaseUserAuthentication(FirebaseToken token) {
            super(List.of(new SimpleGrantedAuthority("ROLE_USER")));
            this.uid = token.getUid();
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return "";
        }

        @Override
        public Object getPrincipal() {
            return uid;
        }

        @Override
        public String getName() {
            return uid;
        }
    }
}
