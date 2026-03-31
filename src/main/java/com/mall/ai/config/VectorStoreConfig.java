package com.mall.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import io.micrometer.observation.ObservationRegistry;
import redis.clients.jedis.JedisPooled;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.embedding.options.model:shaw/dmeta-embedding-zh}")
    private String embeddingModel;

    @Value("${spring.ai.vectorstore.redis.index-name:ai-semantic-cache}")
    private String indexName;

    @Value("${spring.ai.vectorstore.redis.prefix:doc:}")
    private String prefix;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    @Primary
    public EmbeddingModel ollamaEmbeddingModel() {
        OllamaApi ollamaApi = new OllamaApi(ollamaBaseUrl);
        
        OllamaOptions options = OllamaOptions.builder()
                .model(embeddingModel)
                .build();
        
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .observationRegistry(io.micrometer.observation.ObservationRegistry.NOOP)
                .modelManagementOptions(ModelManagementOptions.builder().build())
                .build();
    }

    @Bean
    public RedisVectorStore vectorStore(@org.springframework.beans.factory.annotation.Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
        JedisPooled jedisPooled = new JedisPooled(redisHost, redisPort);

        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName(indexName)
                .prefix(prefix)
                .initializeSchema(true)
                .build();
    }
}
