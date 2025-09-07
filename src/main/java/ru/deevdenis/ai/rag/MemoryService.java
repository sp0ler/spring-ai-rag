package ru.deevdenis.ai.rag;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ru.deevdenis.ai.properties.AiProperties;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    private final RedisVectorStore vectorStore;
    private final AiProperties aiProperties;

    public void saveDocument(Resource resource) {
        log.info("Saving document {}", resource.getFilename());

        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.read();

        AiProperties.Splitter splitter = aiProperties.getSplitter();
        TokenTextSplitter textSplitter = new TokenTextSplitter(
                splitter.getChunkSize(),
                splitter.getMinChars(),
                splitter.getMinTokens(),
                splitter.getMaxChunks(),
                splitter.isKeepSeparator()
        );

        List<Document> notSimilarDocuments = new ArrayList<>(documents.size());
        for (Document document : documents) {
            boolean similarity = isSimilarity(document);
            log.info("Document {} is similar {}", document.getId(), similarity);
            if (!similarity) {
                Map<String, Object> metadata = document.getMetadata();
                metadata.put("createdTime", LocalDateTime.now());
                metadata.put("filename", resource.getFilename());

                notSimilarDocuments.add(document);
            }
        }

        List<Document> split = textSplitter.split(notSimilarDocuments);
        vectorStore.add(split);

        log.info("Saved document {} with count documents {}", resource.getFilename(), notSimilarDocuments.size());
    }

    private boolean isSimilarity(Document document) {
        if (Objects.isNull(document.getText())) {
            return false;
        }

        SearchRequest searchRequest = SearchRequest.builder()
                .query(document.getText())
                .topK(1)
                .similarityThreshold(0.9)
                .build();

        return !vectorStore.similaritySearch(searchRequest).isEmpty();

    }
}
