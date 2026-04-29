package com.petfinder.chat.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import com.petfinder.chat.model.Conversation;
import com.petfinder.chat.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String CONVERSATIONS_COLLECTION = "conversations";
    private static final String MESSAGES_COLLECTION = "messages";

    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    public Conversation createConversation(List<String> participantIds) {
        String id = UUID.randomUUID().toString();
        List<String> names = resolveNames(participantIds);
        Conversation conversation = Conversation.builder()
                .id(id)
                .participantIds(participantIds)
                .participantNames(names)
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
                    .map(this::ensureParticipantNames)
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
        if (message.getRecipientId() == null || message.getRecipientId().isBlank()) {
            String resolved = resolveRecipientId(message.getConversationId(), message.getSenderId());
            if (resolved != null) {
                message.setRecipientId(resolved);
            }
        }

        try {
            getFirestore().collection(MESSAGES_COLLECTION).document(message.getId()).set(message).get();
            getFirestore().collection(CONVERSATIONS_COLLECTION)
                    .document(message.getConversationId())
                    .update("updatedAt", System.currentTimeMillis());
            return message;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error saving message", e);
        }
    }

    public String resolveRecipientId(String conversationId, String senderId) {
        if (conversationId == null || senderId == null) {
            return null;
        }
        try {
            DocumentSnapshot doc = getFirestore()
                    .collection(CONVERSATIONS_COLLECTION)
                    .document(conversationId)
                    .get()
                    .get();
            if (!doc.exists()) {
                return null;
            }
            Conversation conv = doc.toObject(Conversation.class);
            if (conv == null || conv.getParticipantIds() == null) {
                return null;
            }
            return conv.getParticipantIds().stream()
                    .filter(uid -> !Objects.equals(uid, senderId))
                    .findFirst()
                    .orElse(null);
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Could not resolve recipient for conversation {}: {}", conversationId, e.getMessage());
            return null;
        }
    }

    private Conversation ensureParticipantNames(Conversation c) {
        if (c.getParticipantNames() == null || c.getParticipantNames().isEmpty()
                || c.getParticipantNames().size() != c.getParticipantIds().size()) {
            List<String> resolved = resolveNames(c.getParticipantIds());
            c.setParticipantNames(resolved);
            try {
                Map<String, Object> patch = new HashMap<>();
                patch.put("participantNames", resolved);
                getFirestore().collection(CONVERSATIONS_COLLECTION)
                        .document(c.getId())
                        .update(patch);
            } catch (Exception e) {
                log.warn("Could not backfill participantNames for conversation {}: {}", c.getId(), e.getMessage());
            }
        }
        return c;
    }

    private List<String> resolveNames(List<String> uids) {
        if (uids == null) return Collections.emptyList();
        List<String> names = new ArrayList<>(uids.size());
        for (String uid : uids) {
            names.add(resolveName(uid));
        }
        return names;
    }

    private String resolveName(String uid) {
        try {
            UserRecord user = FirebaseAuth.getInstance().getUser(uid);
            if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
                return user.getDisplayName();
            }
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                return user.getEmail();
            }
        } catch (FirebaseAuthException e) {
            log.warn("Could not resolve displayName for uid {}: {}", uid, e.getMessage());
        }
        return uid;
    }
}
