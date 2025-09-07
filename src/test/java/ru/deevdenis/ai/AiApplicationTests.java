package ru.deevdenis.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import ru.deevdenis.ai.rag.MemoryService;
import ru.deevdenis.ai.service.ChatService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiApplicationTests {

	@Value("classpath:annual-report/sber-ar-2023-ru.pdf")
	private Resource sber2023;

	@Value("classpath:annual-report/sber-ar-2024-ru.pdf")
	private Resource sber2024;

	@Autowired
	private ChatService chatService;

	@Autowired
	private MemoryService memoryService;

	@Test
	@DisplayName("Проверка работы чата LLM")
	void checkLLMSuccessTest() {
		var answer = assertDoesNotThrow(() -> chatService.chat("Привет"));
		assertFalse(answer.isEmpty());
	}

}
