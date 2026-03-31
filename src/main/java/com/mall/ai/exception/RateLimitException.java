package com.mall.ai.exception;

/**
 * AI 服务限流异常。
 * 
 * <p>当 DeepSeek API 返回限流错误（HTTP 429）时抛出，
 * 表示请求过于频繁，需要等待后重试。</p>
 * 
 * <p>处理策略：</p>
 * <ul>
 *   <li>WebSocket：推送友好的 fallback 消息</li>
 *   <li>REST API：返回 429 状态码</li>
 * </ul>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
public class RateLimitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造限流异常。
     *
     * @param message 错误信息
     */
    public RateLimitException(String message) {
        super(message);
    }

    /**
     * 构造限流异常（带原因）。
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建默认的限流异常。
     *
     * @return RateLimitException 实例
     */
    public static RateLimitException defaultInstance() {
        return new RateLimitException("AI 服务请求过于频繁，请稍后重试");
    }
}
