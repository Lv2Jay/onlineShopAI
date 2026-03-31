package com.mall.ai.controller;

import com.mall.ai.model.ChatRequest;
import com.mall.ai.model.ChatResponse;
import com.mall.ai.service.AiChatService;
import com.mall.ai.service.SemanticCacheService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AI 聊天 REST 控制器。
 * 
 * <p>提供 HTTP REST API 用于测试 AI 对话功能，
 * 支持同步响应和流式响应两种模式。</p>
 * 
 * <p>API 端点：</p>
 * <ul>
 *   <li>POST /api/chat - 同步聊天接口</li>
 *   <li>POST /api/chat/stream - 流式聊天接口（SSE）</li>
 *   <li>GET /api/health - 健康检查</li>
 * </ul>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final AiChatService aiChatService;
    private final SemanticCacheService semanticCacheService;

    /**
     * 同步聊天接口。
     * 
     * <p>接收用户提问，返回完整的 AI 回复。
     * 适用于简单的问答场景。</p>
     *
     * @param request 聊天请求
     * @return 完整的 AI 回复
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request: sessionId={}, question={}", 
                request.getSessionId(), truncate(request.getQuestion(), 50));

        long startTime = System.currentTimeMillis();
        
        try {
            String response = aiChatService.chat(request);
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("Chat completed: sessionId={}, duration={}ms", 
                    request.getSessionId(), duration);

            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", request.getSessionId());
            result.put("question", request.getQuestion());
            result.put("response", response);
            result.put("duration", duration);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Chat error: sessionId={}", request.getSessionId(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("sessionId", request.getSessionId());
            errorResult.put("error", e.getMessage());
            errorResult.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 流式聊天接口（Server-Sent Events）。
     * 
     * <p>接收用户提问，以 SSE 方式流式返回 AI 回复。
     * 适用于需要打字机效果的前端展示。</p>
     *
     * @param request 聊天请求
     * @return SSE 流式响应
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("Received stream chat request: sessionId={}, question={}", 
                request.getSessionId(), truncate(request.getQuestion(), 50));

        if (request.getSessionId() == null) {
            request.setSessionId("session-" + System.currentTimeMillis());
        }
        if (request.getTimestamp() == null) {
            request.setTimestamp(System.currentTimeMillis());
        }

        return aiChatService.chatStream(request)
                .doOnSubscribe(s -> log.debug("Stream started: sessionId={}", request.getSessionId()))
                .doOnComplete(() -> log.debug("Stream completed: sessionId={}", request.getSessionId()))
                .doOnError(e -> log.error("Stream error: sessionId={}", request.getSessionId(), e))
                .onErrorResume(e -> {
                    log.error("Stream error, returning fallback: sessionId={}", request.getSessionId(), e);
                    return Flux.just("[ERROR] AI服务暂时不可用，请稍后重试");
                });
    }

    /**
     * 简单的 GET 聊天接口（用于快速测试）。
     *
     * @param question 用户提问
     * @return AI 回复
     */
    @GetMapping("/chat")
    public ResponseEntity<Map<String, Object>> chatGet(
            @RequestParam(value = "q", defaultValue = "你好") String question) {
        
        log.info("Received GET chat request: question={}", truncate(question, 50));

        ChatRequest request = ChatRequest.builder()
                .sessionId("test-session-" + System.currentTimeMillis())
                .userId("test-user")
                .question(question)
                .timestamp(System.currentTimeMillis())
                .build();

        long startTime = System.currentTimeMillis();
        
        try {
            String response = aiChatService.chat(request);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> result = new HashMap<>();
            result.put("question", question);
            result.put("response", response);
            result.put("duration", duration);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Chat error", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("question", question);
            errorResult.put("error", e.getMessage());
            errorResult.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 健康检查接口。
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "onlineShopAI");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    /**
     * 清除所有语义缓存。
     *
     * @return 操作结果
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        log.info("Clearing all semantic cache");
        
        try {
            semanticCacheService.clearAllCache();
            long count = semanticCacheService.getCacheCount();
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Semantic cache cleared successfully");
            result.put("remainingEntries", count);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to clear semantic cache", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to clear cache: " + e.getMessage());
            errorResult.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 获取缓存统计信息。
     *
     * @return 缓存条目数量
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        long count = semanticCacheService.getCacheCount();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheEntries", count);
        stats.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * 截断字符串用于日志输出。
     *
     * @param str     原字符串
     * @param maxLen  最大长度
     * @return 截断后的字符串
     */
    private String truncate(String str, int maxLen) {
        if (str == null) {
            return "null";
        }
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
