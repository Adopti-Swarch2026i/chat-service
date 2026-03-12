package com.petfinder.chat.resolver;

import com.petfinder.chat.model.Conversation;
import com.petfinder.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatMutationResolver {

    private final ChatService chatService;

    @MutationMapping
    public Conversation createConversation(@Argument List<String> participantIds) {
        return chatService.createConversation(participantIds);
    }
}
