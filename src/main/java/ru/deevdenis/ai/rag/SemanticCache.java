package ru.deevdenis.ai.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.deevdenis.ai.properties.AiProperties;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static ru.deevdenis.ai.prompts.TemplatePrompts.SEMANTIC_TEMPLATE_PROMPT;

@Component
@RequiredArgsConstructor
public class SemanticCache {

    private final RedisVectorStore vectorStore;
    private final AiProperties aiProperties;
    private final ChatClient chatClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Nullable
    public String findSimilaryRequest(String request) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(request)
                .topK(aiProperties.getSimilarity().getTopK())
                .similarityThreshold(aiProperties.getSimilarity().getThreshold())
                .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        if (documents.isEmpty()) {
            return null;
        } else if (documents.size() == 1) {
            return documents.getFirst().getMetadata().get("response").toString();
        }

        Set<String> similarityText = new HashSet<>();
        for (Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();
            Object value = metadata.get("response");

            if (Objects.nonNull(value) && value instanceof String str) {
                similarityText.add(str);
            }

        }

        Prompt prompt = SEMANTIC_TEMPLATE_PROMPT.create(Map.of("query", request, "context", similarityText));
        return chatClient.prompt(prompt)
                .call()
                .content();
    }

    public long getVectorCount() {
        String searchKey = aiProperties.getEmbedding().getPrefix() + "*";
        Set<String> keys = redisTemplate.keys(searchKey);
        return keys != null ? keys.size() : 0;
    }

    public void saveResponseForSimilarity(String request, String response) {
        Document document = new Document(request, Map.of("response", response, "createdTime", LocalDateTime.now().toString()));
        vectorStore.doAdd(List.of(document));
    }
}
