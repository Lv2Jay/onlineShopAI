package com.mall.ai.service.impl;

import com.mall.ai.model.ChatRequest;
import com.mall.ai.service.AiChatService;
import com.mall.ai.service.SemanticCacheService;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AI 聊天服务实现类。
 * 
 * <p>基于 Spring AI 的 ChatClient 实现，对接 DeepSeek 大模型。
 * 支持流式响应、语义缓存及异常降级处理。</p>
 * 
 * <p>核心流程：</p>
 * <ol>
 *   <li>检查语义缓存，若命中则直接返回</li>
 *   <li>构建 Prompt 并调用 DeepSeek API</li>
 *   <li>流式返回 Token，同时收集完整响应</li>
 *   <li>响应完成后存入语义缓存</li>
 * </ol>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final SemanticCacheService semanticCacheService;

    @Value("${ai.deepseek.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${ai.deepseek.fallback-message:抱歉，AI服务暂时繁忙，请稍后重试。}")
    private String fallbackMessage;

    @Value("${ai.semantic-cache.enabled:true}")
    private boolean semanticCacheEnabled;

    /**
     * 流式聊天实现。
     * 
     * <p>处理流程：</p>
     * <ol>
     *   <li>参数校验与预处理</li>
     *   <li>语义缓存检查</li>
     *   <li>调用大模型获取流式响应</li>
     *   <li>异常处理与降级</li>
     * </ol>
     *
     * @param request 聊天请求
     * @return Token 流式响应
     */
    @Override
    public Flux<String> chatStream(ChatRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            log.warn("Invalid chat request: request is null or question is blank");
            return Flux.just(fallbackMessage);
        }

        log.debug("Processing chat stream request, sessionId: {}, question: {}", 
                request.getSessionId(), truncate(request.getQuestion(), 50));

        if (semanticCacheEnabled) {
            String cachedResponse = semanticCacheService.findSimilarResponse(request.getQuestion());
            if (cachedResponse != null) {
                log.info("Semantic cache hit for sessionId: {}", request.getSessionId());
                return Flux.just(cachedResponse);
            }
        }

        return doChatStream(request)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .onErrorResume(TimeoutException.class, e -> {
                    log.error("Chat stream timeout for sessionId: {}", request.getSessionId(), e);
                    return Flux.just(fallbackMessage);
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Chat stream error for sessionId: {}", request.getSessionId(), e);
                    return Flux.just(fallbackMessage);
                });
    }

    /**
     * 执行实际的流式聊天调用。
     *
     * @param request 聊天请求
     * @return Token 流式响应
     */
    private Flux<String> doChatStream(ChatRequest request) {
        ChatClient chatClient = chatClientBuilder.build();

        StringBuilder fullResponse = new StringBuilder();

        return chatClient.prompt()
                .user(request.getQuestion())
                .stream()
                .content()
                .map(token -> {
                    if (token == null) {
                        return "";
                    }
                    return token.trim();
                })
                .filter(token -> !token.isEmpty())
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    if (semanticCacheEnabled && fullResponse.length() > 0) {
                        semanticCacheService.saveResponse(request.getQuestion(), fullResponse.toString());
                        log.debug("Saved response to semantic cache for sessionId: {}", request.getSessionId());
                    }
                })
                .doOnError(e -> log.error("Error during chat stream for sessionId: {}", 
                        request.getSessionId(), e));
    }

    /**
     * 同步聊天实现。
     *
     * @param request 聊天请求
     * @return 完整的 AI 回复
     */
    @Override
    public String chat(ChatRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            log.warn("Invalid chat request: request is null or question is blank");
            return fallbackMessage;
        }

        log.debug("Processing chat request, sessionId: {}", request.getSessionId());

        if (semanticCacheEnabled) {
            String cachedResponse = semanticCacheService.findSimilarResponse(request.getQuestion());
            if (cachedResponse != null) {
                log.info("Semantic cache hit for sessionId: {}", request.getSessionId());
                return cachedResponse;
            }
        }

        try {
            ChatClient chatClient = chatClientBuilder.build();
            String response = chatClient.prompt()
                    .user(request.getQuestion())
                    .call()
                    .content();

            if (semanticCacheEnabled && response != null && !response.isBlank()) {
                semanticCacheService.saveResponse(request.getQuestion(), response);
            }

            return response != null ? response : fallbackMessage;
        } catch (Exception e) {
            log.error("Chat error for sessionId: {}", request.getSessionId(), e);
            return fallbackMessage;
        }
    }

    /**
     * 检查语义缓存。
     *
     * @param request 聊天请求
     * @return 缓存的响应或 null
     */
    @Override
    public String checkSemanticCache(ChatRequest request) {
        if (!semanticCacheEnabled || request == null || request.getQuestion() == null) {
            return null;
        }
        return semanticCacheService.findSimilarResponse(request.getQuestion());
    }

    /**
     * 存入语义缓存。
     *
     * @param request  聊天请求
     * @param response AI 回复
     */
    @Override
    public void saveToSemanticCache(ChatRequest request, String response) {
        if (!semanticCacheEnabled || request == null || request.getQuestion() == null || response == null) {
            return;
        }
        semanticCacheService.saveResponse(request.getQuestion(), response);
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
