package com.petfinder.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilitar un Message Broker simple en memoria para los prefijos /topic (eventos a clientes)
        config.enableSimpleBroker("/topic");
        
        // El prefijo para que los clientes envíen mensajes hacia la app (ej: /app/chat.send)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registrar el endpoint para iniciar la conexión STOMP, permitiendo CORS
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Soporte para fallback SockJS
        
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // Conexión estándar sin SockJS
    }
}
