package ru.deevdenis.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;
import ru.deevdenis.ai.properties.AiProperties;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(value = {AiProperties.class})
public class AppConfig {

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    public JedisPooled jedisPool(
            JedisConnectionFactory jedisConnectionFactory, @Value("${spring.data.redis.username}") String username
    ) {
        String host = jedisConnectionFactory.getHostName();
        int port = jedisConnectionFactory.getPort();

        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .ssl(jedisConnectionFactory.isUseSsl())
                .clientName(jedisConnectionFactory.getClientName())
                .timeoutMillis(jedisConnectionFactory.getTimeout())
                .password(jedisConnectionFactory.getPassword())
                .user(username)
                .database(jedisConnectionFactory.getDatabase())
                .build();

        return new JedisPooled(new HostAndPort(host, port), clientConfig);
    }

    @Bean
    public RedisVectorStore redisVectorStore(EmbeddingModel embeddingModel, JedisPooled jedisPool, AiProperties aiProperties) {
        return RedisVectorStore.builder(jedisPool, embeddingModel)
                .indexName(aiProperties.getEmbedding().getIndexName())
                .contentFieldName(RedisVectorStore.DEFAULT_CONTENT_FIELD_NAME)
                .embeddingFieldName(RedisVectorStore.DEFAULT_EMBEDDING_FIELD_NAME)
                .metadataFields(
                        RedisVectorStore.MetadataField.text("createdTime"),
                        RedisVectorStore.MetadataField.text("response")
                )
                .prefix(aiProperties.getEmbedding().getPrefix())
                .initializeSchema(true)
                .vectorAlgorithm(RedisVectorStore.Algorithm.HSNW)
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        return template;
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return new TransformersEmbeddingModel();
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
