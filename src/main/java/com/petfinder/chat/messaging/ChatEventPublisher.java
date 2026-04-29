package com.petfinder.chat.messaging;

import com.petfinder.chat.config.RabbitMQConfig;
import com.petfinder.chat.model.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publica eventos de chat al exchange topic 'adopti.events'.
 *
 * Convención (sección 7.3 de p2_plan.md):
 *  - El cuerpo del mensaje contiene únicamente los campos del catálogo
 *    (no se usa envelope), para que los consumers deserialicen al schema
 *    del contrato directamente.
 *  - eventId (UUID v4) y eventTimestamp (ISO 8601) viajan en los headers
 *    AMQP y también se duplican en messageId / timestamp para herramientas
 *    estándar.
 */
@Component
@RequiredArgsConstructor
public class ChatEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatEventPublisher.class);
    private static final String ROUTING_KEY = "chat.message.sent";
    private static final int CONTENT_PREVIEW_MAX_LENGTH = 100;

    private final RabbitTemplate rabbitTemplate;

    public void publishMessageSent(Message message) {
        if (message == null) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", message.getConversationId());
        payload.put("senderId", message.getSenderId());
        payload.put("recipientId", message.getRecipientId());
        payload.put("contentPreview", truncate(message.getContent(), CONTENT_PREVIEW_MAX_LENGTH));
        payload.put("timestamp", message.getTimestamp());

        String eventId = UUID.randomUUID().toString();
        String eventTimestamp = Instant.now().toString();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    ROUTING_KEY,
                    payload,
                    amqpMessage -> {
                        MessageProperties props = amqpMessage.getMessageProperties();
                        props.setMessageId(eventId);
                        props.setTimestamp(Date.from(Instant.now()));
                        props.setContentEncoding("UTF-8");
                        props.setHeader("eventId", eventId);
                        props.setHeader("eventTimestamp", eventTimestamp);
                        return amqpMessage;
                    }
            );
            log.info("Published {} (eventId={}) for conversation {}",
                    ROUTING_KEY, eventId, message.getConversationId());
        } catch (Exception e) {
            log.warn("Failed to publish {}: {}", ROUTING_KEY, e.getMessage());
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
