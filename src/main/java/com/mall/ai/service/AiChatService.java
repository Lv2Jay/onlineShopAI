package com.mall.ai.service;

import com.mall.ai.model.ChatRequest;
import reactor.core.publisher.Flux;

/**
 * AI 聊天服务接口。
 * 
 * <p>定义与 AI 大模型交互的核心能力，支持流式响应与语义缓存。</p>
 * 
 * <p>实现类应处理以下场景：</p>
 * <ul>
 *   <li>语义缓存命中：直接返回缓存的完整响应</li>
 *   <li>大模型调用：通过 Spring AI 调用 DeepSeek API</li>
 *   <li>异常处理：超时、限流等异常的优雅降级</li>
 * </ul>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
public interface AiChatService {

    /**
     * 流式聊天接口。
     * 
     * <p>接收用户提问，返回 Token 级别的流式响应。
     * 内部会先检查语义缓存，若命中则返回单元素 Flux。</p>
     *
     * @param request 聊天请求，包含用户提问与会话信息
     * @return Token 流式响应，每个元素为单个 Token 字符串
     */
    Flux<String> chatStream(ChatRequest request);

    /**
     * 同步聊天接口（非流式）。
     * 
     * <p>接收用户提问，返回完整的 AI 回复。
     * 适用于不支持流式处理的场景。</p>
     *
     * @param request 聊天请求
     * @return 完整的 AI 回复内容
     */
    String chat(ChatRequest request);

    /**
     * 检查是否命中语义缓存。
     *
     * @param request 聊天请求
     * @return 命中时返回缓存的响应，否则返回 null
     */
    String checkSemanticCache(ChatRequest request);

    /**
     * 将问答对存入语义缓存。
     *
     * @param request  聊天请求
     * @param response AI 回复内容
     */
    void saveToSemanticCache(ChatRequest request, String response);
}
