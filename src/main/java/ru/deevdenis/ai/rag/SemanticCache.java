package ru.deevdenis.ai.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.deevdenis.ai.properties.AiProperties;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SemanticCache {

    private static final String SEMANTIC_CACHE_KEY = "response";

    private final RedisVectorStore vectorStore;
    private final AiProperties aiProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    @Nullable
    public String findSimilarityRequest(SearchRequest request) {
        List<Document> documents = vectorStore.similaritySearch(request);

        if (documents.isEmpty()) {
            return null;
        } else if (documents.size() == 1) {
            return documents.getFirst().getMetadata().get(SEMANTIC_CACHE_KEY).toString();
        }

        Set<String> similarityText = new HashSet<>();
        for (Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();
            Object value = metadata.get(SEMANTIC_CACHE_KEY);

            if (Objects.nonNull(value) && value instanceof String str) {
                similarityText.add(str);
            }
        }

        return String.join(",", similarityText);
    }

    public long getVectorCount() {
        String searchKey = aiProperties.getEmbedding().getPrefix() + "*";
        Set<String> keys = redisTemplate.keys(searchKey);
        return keys != null ? keys.size() : 0;
    }

    public void saveResponseForSimilarity(String request, String response) {
        Document document = new Document(request, Map.of(SEMANTIC_CACHE_KEY, response, "createdTime", LocalDateTime.now().toString()));
        vectorStore.doAdd(List.of(document));
    }
}
