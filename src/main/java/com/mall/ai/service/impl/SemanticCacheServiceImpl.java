package com.mall.ai.service.impl;

import com.mall.ai.service.SemanticCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class SemanticCacheServiceImpl implements SemanticCacheService {

    private final VectorStore vectorStore;
    private final double similarityThreshold;
    private final Duration ttl;
    private final boolean enabled;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    public SemanticCacheServiceImpl(
            VectorStore vectorStore,
            @Value("${ai.semantic-cache.similarity-threshold:0.92}") double similarityThreshold,
            @Value("${ai.semantic-cache.ttl-seconds:3600}") int ttlSeconds,
            @Value("${ai.semantic-cache.enabled:true}") boolean enabled,
            org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = similarityThreshold;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.enabled = enabled;
        this.redisTemplate = redisTemplate;
        log.info("SemanticCacheService initialized: enabled={}, threshold={}, ttl={}", 
                enabled, similarityThreshold, ttl);
    }

    @Override
    public String findSimilarResponse(String question) {
        if (!enabled) {
            return null;
        }
        
        if (question == null || question.isBlank()) {
            log.warn("Question is null or blank, returning null");
            return null;
        }

        try {
            log.debug("Searching for similar question: {}", truncate(question, 50));

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(question)
                    .topK(1)
                    .similarityThreshold(similarityThreshold)
                    .build();

            List<org.springframework.ai.document.Document> documents = 
                    vectorStore.similaritySearch(searchRequest);

            if (documents != null && !documents.isEmpty()) {
                org.springframework.ai.document.Document doc = documents.get(0);
                String response = String.valueOf(doc.getMetadata().get("response"));
                Object scoreObj = doc.getMetadata().get("score");
                String scoreStr = (scoreObj != null) ? String.valueOf(scoreObj) : "N/A";
                String source = String.valueOf(doc.getMetadata().getOrDefault("source", "unknown"));

                log.info("Semantic cache hit: similarity={}, source={}, question={}",
                        scoreStr, source, truncate(doc.getText(), 30));
                return response;
            }

            log.debug("No similar question found in cache");
            return null;
        } catch (Exception e) {
            log.error("Error searching semantic cache for question: {}", 
                    truncate(question, 30), e);
            return null;
        }
    }

    @Override
    public void saveResponse(String question, String response) {
        if (!enabled) {
            return;
        }
        
        if (question == null || question.isBlank() || response == null || response.isBlank()) {
            log.warn("Invalid question or response, skipping save");
            return;
        }

        try {
            org.springframework.ai.document.Document document = 
                    new org.springframework.ai.document.Document(
                            question,
                            java.util.Map.of(
                                    "response", response,
                                    "source", "semantic-cache",
                                    "timestamp", String.valueOf(System.currentTimeMillis())
                            )
                    );

            vectorStore.add(List.of(document));
            log.info("Saved response to semantic cache: question={}", truncate(question, 30));
        } catch (Exception e) {
            log.error("Error saving to semantic cache: question={}", truncate(question, 30), e);
        }
    }

    @Override
    public void clearAllCache() {
        if (!enabled) {
            log.warn("Semantic cache is disabled, skipping clear operation");
            return;
        }
        
        try {
            // 先获取所有缓存的文档
            SearchRequest searchRequest = SearchRequest.builder()
                    .query("")
                    .topK(10000)
                    .similarityThreshold(0.0)
                    .build();
            
            List<org.springframework.ai.document.Document> documents = 
                    vectorStore.similaritySearch(searchRequest);
            
            if (documents != null && !documents.isEmpty()) {
                // 提取所有文档 ID
                List<String> ids = documents.stream()
                        .map(org.springframework.ai.document.Document::getId)
                        .filter(id -> id != null && !id.isBlank())
                        .toList();
                
                if (!ids.isEmpty()) {
                    // 删除所有文档
                    vectorStore.delete(ids);
                    log.info("Deleted {} documents from semantic cache", ids.size());
                }
            }
            
            // 额外清理：直接删除 Redis 中的索引键
            String indexName = "ai-semantic-cache"; // 从配置文件读取或使用默认值
            Set<String> keys = redisTemplate.keys("doc:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Deleted {} Redis keys with pattern 'doc:*'", keys.size());
            }
            
            log.info("Cleared all semantic cache entries");
        } catch (Exception e) {
            log.error("Error clearing semantic cache", e);
            throw new RuntimeException("Failed to clear semantic cache: " + e.getMessage(), e);
        }
    }

    @Override
    public long getCacheCount() {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query("")
                    .topK(1000)
                    .similarityThreshold(0.0)
                    .build();
            List<org.springframework.ai.document.Document> documents = 
                    vectorStore.similaritySearch(searchRequest);
            return documents != null ? documents.size() : 0;
        } catch (Exception e) {
            log.error("Error getting cache count", e);
            return -1;
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) {
            return "null";
        }
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
