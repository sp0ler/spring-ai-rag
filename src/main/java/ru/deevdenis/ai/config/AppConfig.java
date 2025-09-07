package ru.deevdenis.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.DefaultRedisCredentials;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

@Configuration
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
    public RedisVectorStore redisVectorStore(EmbeddingModel embeddingModel, JedisPooled jedisPool) {
        return RedisVectorStore.builder(jedisPool, embeddingModel)
                .indexName(RedisVectorStore.DEFAULT_INDEX_NAME)
                .contentFieldName(RedisVectorStore.DEFAULT_CONTENT_FIELD_NAME)
                .embeddingFieldName(RedisVectorStore.DEFAULT_EMBEDDING_FIELD_NAME)
                .metadataFields(
                        RedisVectorStore.MetadataField.text("filename"),
                        RedisVectorStore.MetadataField.text("createdTime")
                )
                .prefix(RedisVectorStore.DEFAULT_PREFIX)
                .initializeSchema(true)
                .vectorAlgorithm(RedisVectorStore.Algorithm.HSNW)
                .build();
    }
}
