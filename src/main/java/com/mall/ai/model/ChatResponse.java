package com.mall.ai.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 聊天响应模型。
 * 
 * <p>封装通过 WebSocket 推送回前端的响应数据，
 * 支持流式 Token 推送与完整响应两种模式。</p>
 * 
 * <p>响应类型：</p>
 * <ul>
 *   <li>{@code TOKEN} - 单个 Token 流式推送</li>
 *   <li>{@code COMPLETE} - 完整响应结束标识</li>
 *   <li>{@code ERROR} - 错误响应</li>
 *   <li>{@code CACHED} - 缓存命中响应</li>
 * </ul>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应类型枚举。
     */
    public enum ResponseType {
        /** 单个 Token 流式推送 */
        TOKEN,
        /** 完整响应结束标识 */
        COMPLETE,
        /** 错误响应 */
        ERROR,
        /** 缓存命中响应 */
        CACHED
    }

    /**
     * 会话唯一标识。
     * 与请求中的 sessionId 对应，用于前端关联响应。
     */
    private String sessionId;

    /**
     * 响应类型。
     */
    private ResponseType type;

    /**
     * 响应内容。
     * 流式模式下为单个 Token，完整模式下为整体回复。
     */
    private String content;

    /**
     * 是否来自缓存。
     * {@code true} 表示命中语义缓存，响应更快。
     */
    private boolean fromCache;

    /**
     * 错误码（仅当 type=ERROR 时有效）。
     */
    private String errorCode;

    /**
     * 错误信息（仅当 type=ERROR 时有效）。
     */
    private String errorMessage;

    /**
     * 响应时间戳（毫秒）。
     */
    private Long timestamp;

    /**
     * 创建 Token 流式响应。
     *
     * @param sessionId 会话标识
     * @param token     Token 内容
     * @return ChatResponse 实例
     */
    public static ChatResponse token(String sessionId, String token) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .type(ResponseType.TOKEN)
                .content(token)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建完成响应。
     *
     * @param sessionId 会话标识
     * @return ChatResponse 实例
     */
    public static ChatResponse complete(String sessionId) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .type(ResponseType.COMPLETE)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建错误响应。
     *
     * @param sessionId    会话标识
     * @param errorCode    错误码
     * @param errorMessage 错误信息
     * @return ChatResponse 实例
     */
    public static ChatResponse error(String sessionId, String errorCode, String errorMessage) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .type(ResponseType.ERROR)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建缓存命中响应。
     *
     * @param sessionId 会话标识
     * @param content   缓存的完整回复内容
     * @return ChatResponse 实例
     */
    public static ChatResponse cached(String sessionId, String content) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .type(ResponseType.CACHED)
                .content(content)
                .fromCache(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
