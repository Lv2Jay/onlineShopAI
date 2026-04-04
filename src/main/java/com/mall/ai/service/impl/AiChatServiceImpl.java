package com.mall.ai.service.impl;

import com.mall.ai.annotation.SemanticCacheable;
import com.mall.ai.model.ChatRequest;
import com.mall.ai.service.AiChatService;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 聊天服务实现类。
 *
 * <p>基于 Spring AI 的 ChatClient 实现，对接 DeepSeek 大模型。
 * 核心职责是与 AI 模型交互，语义缓存功能由 AOP 切面处理。</p>
 *
 * <p>架构说明：</p>
 * <ul>
 *   <li>本类专注于 AI 对话核心逻辑</li>
 *   <li>使用 @SemanticCacheable 注解声明缓存需求</li>
 *   <li>缓存检查与保存由 SemanticCacheAspect 自动处理</li>
 * </ul>
 *
 * @author onlineShopAI Team
 * @version 1.0.0
 * @see SemanticCacheable
 * @see com.mall.ai.aspect.SemanticCacheAspect
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;

    public AiChatServiceImpl(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Value("${ai.deepseek.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${ai.deepseek.fallback-message:抱歉，AI服务暂时繁忙，请稍后重试。}")
    private String fallbackMessage;

    /**
     * 流式聊天实现。
     *
     * <p>核心流程：</p>
     * <ol>
     *   <li>参数校验</li>
     *   <li>调用 ChatClient 获取流式响应</li>
     *   <li>异常处理与降级</li>
     * </ol>
     *
     * <p>缓存处理由 {@link SemanticCacheable} 注解声明，
     * 由 {@link com.mall.ai.aspect.SemanticCacheAspect} AOP 切面执行</p>
     *
     * @param request 聊天请求
     * @return Token 流式响应
     */
    @Override
    @SemanticCacheable(checkCache = true, cacheResult = true)
    public Flux<String> chatStream(ChatRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            log.warn("Invalid chat request: request is null or question is blank");
            return Flux.just(fallbackMessage);
        }

        log.debug("Processing chat stream request, sessionId: {}, question: {}",
                request.getSessionId(), truncate(request.getQuestion(), 50));

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
        return chatClient.prompt()
                .user(request.getQuestion())
                .stream()
                .content()
                .map(token -> token == null ? "" : token.trim())
                .filter(token -> !token.isEmpty());
    }

    /**
     * 同步聊天实现。
     *
     * <p>核心流程：</p>
     * <ol>
     *   <li>参数校验</li>
     *   <li>调用 ChatClient 获取响应</li>
     *   <li>异常处理与降级</li>
     * </ol>
     *
     * <p>缓存处理由 {@link SemanticCacheable} 注解声明，
     * 由 {@link com.mall.ai.aspect.SemanticCacheAspect} AOP 切面执行</p>
     *
     * @param request 聊天请求
     * @return 完整的 AI 回复
     */
    @Override
    @SemanticCacheable(checkCache = true, cacheResult = true)
    public String chat(ChatRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            log.warn("Invalid chat request: request is null or question is blank");
            return fallbackMessage;
        }

        log.debug("Processing chat request, sessionId: {}", request.getSessionId());

        try {
            String response = chatClient.prompt()
                    .user(request.getQuestion())
                    .call()
                    .content();

            return response != null && !response.isBlank() ? response : fallbackMessage;
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
        return null;
    }

    /**
     * 将问答对存入语义缓存。
     *
     * <p>注：此方法已废弃，缓存逻辑由 AOP 切面自动处理。</p>
     *
     * @param request  聊天请求
     * @param response AI 回复内容
     */
    @Override
    public void saveToSemanticCache(ChatRequest request, String response) {
    }

    private String truncate(String str, int maxLen) {
        if (str == null) {
            return "null";
        }
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
