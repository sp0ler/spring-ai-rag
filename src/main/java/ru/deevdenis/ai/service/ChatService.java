package ru.deevdenis.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import ru.deevdenis.ai.rag.RedisMemoryAdvisor;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final RedisMemoryAdvisor redisMemoryAdvisor;

    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(redisMemoryAdvisor)
                .call()
                .content();
    }
}
