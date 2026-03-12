package com.petfinder.chat.controller;

import com.petfinder.chat.model.Message;
import com.petfinder.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload Message message) {
        // En un entorno de producción, validamos el token de User asociado al Principal aquí.
        
        // Asignamos server timestamp y generamos UUID si no tiene
        Message savedMessage = chatService.saveMessage(message);

        // Distribuimos el mensaje a todos los subscritos en ese canal (Topic) de conversación
        String destination = "/topic/chat/" + savedMessage.getConversationId();
        messagingTemplate.convertAndSend(destination, savedMessage);
    }
}
