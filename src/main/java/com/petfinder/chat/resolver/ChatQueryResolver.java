package com.petfinder.chat.resolver;

import com.petfinder.chat.model.Conversation;
import com.petfinder.chat.model.Message;
import com.petfinder.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatQueryResolver {

    private final ChatService chatService;

    @QueryMapping
    public List<Conversation> conversations(@Argument String userId) {
        return chatService.getConversations(userId);
    }

    @QueryMapping
    public List<Message> messages(@Argument String conversationId) {
        return chatService.getMessages(conversationId);
    }
}
