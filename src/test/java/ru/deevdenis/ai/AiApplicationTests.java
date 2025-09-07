package ru.deevdenis.ai;

import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import ru.deevdenis.ai.properties.AiProperties;
import ru.deevdenis.ai.rag.MemoryService;
import ru.deevdenis.ai.rag.SemanticCache;
import ru.deevdenis.ai.service.ChatService;

import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AiApplicationTests {

	@Value("classpath:annual-report/sber-ar-2023-ru.pdf")
	protected Resource sber2023;

	@Value("classpath:annual-report/sber-ar-2024-ru.pdf")
	protected Resource sber2024;

	@Autowired
	protected ChatService chatService;

	@Autowired
	protected MemoryService memoryService;

	@Autowired
	protected RedisVectorStore vectorStore;

	@Autowired
	protected RedisTemplate<String, Object> redisTemplate;

	@Autowired
	protected SemanticCache semanticCache;

	@Autowired
	protected AiProperties aiProperties;

	public void deleteAllViaRedis() {
		String prefix = aiProperties.getEmbedding().getPrefix() + "*";
		Set<String> keys = redisTemplate.keys(prefix);

		if (!CollectionUtils.isEmpty(keys)) {
			redisTemplate.delete(keys);
		}
	}
}
