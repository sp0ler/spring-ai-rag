package ru.deevdenis.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.deevdenis.ai.AiApplicationTests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ChatServiceTest extends AiApplicationTests {

    @Test
    @DisplayName("Проверка работы чата LLM")
    void checkLLMSuccessTest() {
        var answer = assertDoesNotThrow(() -> chatService.chat("Привет"));
        assertFalse(answer.isEmpty());
    }
}
