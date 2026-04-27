package com.petfinder.chat.messaging;

import com.petfinder.chat.config.RabbitMQConfig;
import com.petfinder.chat.model.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public void publishMessageSent(Message message) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventTimestamp", Instant.now().toString());
        event.put("routingKey", "chat.message.sent");

        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", message.getConversationId());
        data.put("senderId", message.getSenderId());
        data.put("contentPreview", truncate(message.getContent(), 100));
        data.put("timestamp", message.getTimestamp());
        event.put("data", data);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    "chat.message.sent",
                    event
            );
            log.info("Published chat.message.sent for conversation {}", message.getConversationId());
        } catch (Exception e) {
            log.warn("Failed to publish chat.message.sent: {}", e.getMessage());
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
