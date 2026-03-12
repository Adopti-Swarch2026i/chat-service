package com.petfinder.chat.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.petfinder.chat.model.Conversation;
import com.petfinder.chat.model.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final String CONVERSATIONS_COLLECTION = "conversations";
    private static final String MESSAGES_COLLECTION = "messages";

    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    public Conversation createConversation(List<String> participantIds) {
        String id = UUID.randomUUID().toString();
        Conversation conversation = Conversation.builder()
                .id(id)
                .participantIds(participantIds)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        try {
            getFirestore().collection(CONVERSATIONS_COLLECTION).document(id).set(conversation).get();
            return conversation;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error creating conversation in Firestore", e);
        }
    }

    public List<Conversation> getConversations(String userId) {
        try {
            ApiFuture<QuerySnapshot> future = getFirestore()
                    .collection(CONVERSATIONS_COLLECTION)
                    .whereArrayContains("participantIds", userId)
                    .get();

            return future.get().getDocuments().stream()
                    .map(doc -> doc.toObject(Conversation.class))
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching conversations", e);
        }
    }

    public List<Message> getMessages(String conversationId) {
        try {
            ApiFuture<QuerySnapshot> future = getFirestore()
                    .collection(MESSAGES_COLLECTION)
                    .whereEqualTo("conversationId", conversationId)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get();

            return future.get().getDocuments().stream()
                    .map(doc -> doc.toObject(Message.class))
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching messages", e);
        }
    }

    public Message saveMessage(Message message) {
        if (message.getId() == null || message.getId().isEmpty()) {
            message.setId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(System.currentTimeMillis());
        }

        try {
            // Guardar el mensaje
            getFirestore().collection(MESSAGES_COLLECTION).document(message.getId()).set(message).get();
            
            // Actualizar la última vez que la conversación tuvo un movimiento
            // de forma asíncrona pero esperando el primer set
            getFirestore().collection(CONVERSATIONS_COLLECTION)
                    .document(message.getConversationId())
                    .update("updatedAt", System.currentTimeMillis());

            return message;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error saving message", e);
        }
    }
}
