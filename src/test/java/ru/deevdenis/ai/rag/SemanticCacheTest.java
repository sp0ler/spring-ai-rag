package ru.deevdenis.ai.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import ru.deevdenis.ai.AiApplicationTests;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticCacheTest extends AiApplicationTests {

    @BeforeEach
    void setUp() {
        deleteAllViaRedis();
    }

    @Test
    @DisplayName("Проверка работы семантического кэша с отрицательным результатом")
    void findSimilarlyNullResultSuccessTest() {
        SearchRequest searchRequest = SearchRequest.builder()
                .query("Я хочу найти информацию о том, как сделать кашу")
                .topK(aiProperties.getSimilarity().getTopK())
                .similarityThreshold(aiProperties.getSimilarity().getThreshold())
                .build();

        String result = assertDoesNotThrow(() -> semanticCache.findSimilarityRequest(searchRequest));
        assertTrue(Objects.isNull(result));
    }

    @Test
    @DisplayName("Проверка количества векторов в семантическом кэше")
    void checkVectorCountSuccessTest() {
        long count = assertDoesNotThrow(() -> semanticCache.getVectorCount());
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Проверка сохранения семантического кэша")
    void saveSimilarlySuccess() {
        assertDoesNotThrow(() -> semanticCache.saveResponseForSimilarity(
                "Я хочу найти информацию о том, как сделать кашу",
                "Я не знаю, как это сделать"
        ));

        long count = semanticCache.getVectorCount();
        assertEquals(1, count);
    }

    @Test
    @DisplayName("Проверка сохранения семантического кэша с ответом")
    void findSimilarlySuccessTest() {
        String request = "Какая сегодня погода в Москве?";
        String response = "Сегодня в Москце +15°C, переменная облачность, без осадков";

        semanticCache.saveResponseForSimilarity(request, response);

        String result = chatService.chat("Какая погода в Москве?");
        assertTrue(result.contains("+15"));
    }

    @Test
    @DisplayName("Проверка сохранения семантического кэша вместе с LLM")
    void findSimilarlyWithLLMSuccessTest() {
        semanticCache.saveResponseForSimilarity(
                "Какая сегодня погода в Москве?",
                "Сегодня в Москце +15°C, переменная облачность, без осадков."
        );

        semanticCache.saveResponseForSimilarity(
                "Что по погоде в Москве?",
                "В Москве сейчас +15 градусов, местами облачно, дождя нет."
        );

        String result = chatService.chat("Какая погода в Москве?");
        assertTrue(result.contains("+15"));

    }
}
