package com.petfinder.chat.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;

@Service
public class FirebaseAuthService {

    /**
     * Valida manualmente un token JWT de Firebase usando Firebase Admin SDK.
     * Útil para interceptores de WebSocket donde Spring Security HTTP Filter
     * no se activa por defecto (ej. paso de token en header nativo de STOMP).
     *
     * @param token String JWT a validar
     * @return FirebaseToken conteniendo claims e información de UUID del user
     * @throws FirebaseAuthException si es inválido
     */
    public FirebaseToken verifyToken(String token) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().verifyIdToken(token);
    }
}
