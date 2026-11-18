package com.trustdoc.chat;

import com.trustdoc.auth.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    public record Question(@NotBlank @Size(max = 500) String question) {}

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @PostMapping
    public ChatService.ChatAnswer ask(@Valid @RequestBody Question q) {
        return chat.ask(q.question().trim(), CurrentUser.get());
    }
}
