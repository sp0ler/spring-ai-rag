package ru.deevdenis.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import ru.deevdenis.ai.rag.SemanticCache;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final SemanticCache semanticCache;

    public String chat(String message) {
        String answer = semanticCache.findSimilaryRequest(message);
        if (StringUtils.isNoneEmpty(answer)) {
            return answer;
        }

        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
