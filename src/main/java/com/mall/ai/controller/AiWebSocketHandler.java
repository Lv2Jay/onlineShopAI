package com.mall.ai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.ai.model.ChatRequest;
import com.mall.ai.model.ChatResponse;
import com.mall.ai.service.AiChatService;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * AI WebSocket 处理器。
 * 
 * <p>处理前端 WebSocket 连接，实现 AI 对话的流式响应推送。
 * 支持打字机效果的前端展示体验。</p>
 * 
 * <p>核心功能：</p>
 * <ul>
 *   <li>连接管理：维护活跃的 WebSocket 会话</li>
 *   <li>消息解析：接收并解析前端 JSON 消息</li>
 *   <li>流式推送：将 AI Token 流实时推送到前端</li>
 *   <li>资源清理：连接断开时清理订阅和会话资源</li>
 * </ul>
 * 
 * <p>通信协议：</p>
 * <pre>{@code
 * // 请求格式
 * {
 *   "sessionId": "session-123",
 *   "userId": "user-456",
 *   "question": "这款手机的续航能力如何？"
 * }
 * 
 * // 响应格式（Token 推送）
 * {
 *   "sessionId": "session-123",
 *   "type": "TOKEN",
 *   "content": "这",
 *   "timestamp": 1712345678901
 * }
 * 
 * // 响应格式（完成标识）
 * {
 *   "sessionId": "session-123",
 *   "type": "COMPLETE",
 *   "timestamp": 1712345678902
 * }
 * }</pre>
 * 
 * <p>防御性编程要点：</p>
 * <ul>
 *   <li>连接断开时必须清理订阅，防止内存泄漏</li>
 *   <li>异常时推送友好的 fallback 消息</li>
 *   <li>会话资源使用 ConcurrentHashMap 管理并发安全</li>
 * </ul>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiWebSocketHandler extends TextWebSocketHandler {

    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Disposable> subscriptions = new ConcurrentHashMap<>();

    /**
     * WebSocket 连接建立回调。
     * 
     * <p>将新会话加入活跃会话映射，便于后续消息推送和资源管理。</p>
     *
     * @param session WebSocket 会话
     * @throws Exception 连接处理异常
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Objects.requireNonNull(session, "WebSocket session must not be null");
        
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        
        log.info("WebSocket connection established: sessionId={}, remoteAddress={}", 
                sessionId, session.getRemoteAddress());
        
        super.afterConnectionEstablished(session);
    }

    /**
     * WebSocket 消息接收处理。
     * 
     * <p>解析前端发送的 JSON 消息，调用 AI 服务获取流式响应，
     * 并通过 WebSocket 实时推送 Token 到前端。</p>
     *
     * @param session WebSocket 会话
     * @param message 文本消息
     * @throws Exception 消息处理异常
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Objects.requireNonNull(session, "WebSocket session must not be null");
        Objects.requireNonNull(message, "TextMessage must not be null");
        
        String payload = message.getPayload();
        log.debug("Received WebSocket message: sessionId={}, payload={}", 
                session.getId(), truncate(payload, 100));

        ChatRequest request;
        try {
            request = objectMapper.readValue(payload, ChatRequest.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse chat request: sessionId={}, payload={}", 
                    session.getId(), truncate(payload, 50), e);
            sendErrorResponse(session, null, "PARSE_ERROR", "消息格式错误，请检查 JSON 格式");
            return;
        }

        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            log.warn("Empty question received: sessionId={}", session.getId());
            sendErrorResponse(session, request.getSessionId(), "EMPTY_QUESTION", "问题内容不能为空");
            return;
        }

        if (request.getSessionId() == null) {
            request.setSessionId(session.getId());
        }
        if (request.getTimestamp() == null) {
            request.setTimestamp(System.currentTimeMillis());
        }

        processChatStream(session, request);
    }

    /**
     * 处理流式聊天响应。
     * 
     * <p>订阅 AI 服务的 Token 流，逐个推送到前端，
     * 并在完成时发送 COMPLETE 标识。</p>
     *
     * @param session WebSocket 会话
     * @param request  聊天请求
     */
    private void processChatStream(WebSocketSession session, ChatRequest request) {
        String sessionId = request.getSessionId();
        
        cancelExistingSubscription(sessionId);

        Flux<String> tokenFlux = aiChatService.chatStream(request);

        Disposable subscription = tokenFlux
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        token -> sendTokenToClient(session, sessionId, token),
                        error -> handleStreamError(session, sessionId, error),
                        () -> sendCompleteToClient(session, sessionId)
                );

        subscriptions.put(sessionId, subscription);
    }

    /**
     * 取消已有的订阅。
     * 
     * <p>防止同一会话的重复订阅导致资源泄漏。</p>
     *
     * @param sessionId 会话标识
     */
    private void cancelExistingSubscription(String sessionId) {
        Disposable existing = subscriptions.remove(sessionId);
        if (existing != null && !existing.isDisposed()) {
            existing.dispose();
            log.debug("Cancelled existing subscription for sessionId: {}", sessionId);
        }
    }

    /**
     * 发送 Token 到客户端。
     *
     * @param session   WebSocket 会话
     * @param sessionId 会话标识
     * @param token     Token 内容
     */
    private void sendTokenToClient(WebSocketSession session, String sessionId, String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        
        try {
            ChatResponse response = ChatResponse.token(sessionId, token);
            String json = objectMapper.writeValueAsString(response);
            
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.error("Failed to send token to client: sessionId={}", sessionId, e);
        }
    }

    /**
     * 发送完成标识到客户端。
     *
     * @param session   WebSocket 会话
     * @param sessionId 会话标识
     */
    private void sendCompleteToClient(WebSocketSession session, String sessionId) {
        try {
            ChatResponse response = ChatResponse.complete(sessionId);
            String json = objectMapper.writeValueAsString(response);
            
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
            
            log.debug("Sent complete signal to client: sessionId={}", sessionId);
        } catch (IOException e) {
            log.error("Failed to send complete signal: sessionId={}", sessionId, e);
        } finally {
            subscriptions.remove(sessionId);
        }
    }

    /**
     * 处理流式响应错误。
     *
     * @param session   WebSocket 会话
     * @param sessionId 会话标识
     * @param error     错误对象
     */
    private void handleStreamError(WebSocketSession session, String sessionId, Throwable error) {
        log.error("Stream error for sessionId: {}", sessionId, error);
        
        String errorMessage = "AI 服务暂时不可用，请稍后重试";
        
        if (error.getMessage() != null) {
            if (error.getMessage().contains("rate limit") || error.getMessage().contains("429")) {
                errorMessage = "AI 服务繁忙，请稍后重试";
            } else if (error.getMessage().contains("timeout")) {
                errorMessage = "请求超时，请稍后重试";
            }
        }
        
        sendErrorResponse(session, sessionId, "AI_ERROR", errorMessage);
        subscriptions.remove(sessionId);
    }

    /**
     * 发送错误响应到客户端。
     *
     * @param session      WebSocket 会话
     * @param sessionId    会话标识
     * @param errorCode    错误码
     * @param errorMessage 错误信息
     */
    private void sendErrorResponse(WebSocketSession session, String sessionId, 
                                    String errorCode, String errorMessage) {
        try {
            ChatResponse response = ChatResponse.error(sessionId, errorCode, errorMessage);
            String json = objectMapper.writeValueAsString(response);
            
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.error("Failed to send error response: sessionId={}", sessionId, e);
        }
    }

    /**
     * WebSocket 连接关闭回调。
     * 
     * <p>关键资源清理步骤：</p>
     * <ol>
     *   <li>取消该会话的所有订阅，防止内存泄漏</li>
     *   <li>从会话映射中移除</li>
     *   <li>关闭 WebSocket 连接</li>
     * </ol>
     *
     * @param session WebSocket 会话
     * @param status  关闭状态
     * @throws Exception 关闭处理异常
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Objects.requireNonNull(session, "WebSocket session must not be null");
        
        String sessionId = session.getId();
        
        Disposable subscription = subscriptions.remove(sessionId);
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.debug("Disposed subscription for closed session: sessionId={}", sessionId);
        }
        
        sessions.remove(sessionId);
        
        log.info("WebSocket connection closed: sessionId={}, status={}", sessionId, status);
        
        super.afterConnectionClosed(session, status);
    }

    /**
     * WebSocket 传输错误回调。
     *
     * @param session   WebSocket 会话
     * @param exception 传输异常
     * @throws Exception 错误处理异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session != null ? session.getId() : "unknown";
        log.error("WebSocket transport error: sessionId={}", sessionId, exception);
        
        if (session != null && session.isOpen()) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException e) {
                log.error("Failed to close session after transport error: sessionId={}", sessionId, e);
            }
        }
        
        super.handleTransportError(session, exception);
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
