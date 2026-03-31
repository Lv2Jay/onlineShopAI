package com.mall.ai.exception;

/**
 * AI 服务超时异常。
 * 
 * <p>当 DeepSeek API 调用超时时抛出，
 * 表示服务响应时间超过配置的超时阈值。</p>
 * 
 * <p>处理策略：</p>
 * <ul>
 *   <li>WebSocket：推送超时 fallback 消息</li>
 *   <li>REST API：返回 504 Gateway Timeout</li>
 * </ul>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
public class AiTimeoutException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造超时异常。
     *
     * @param message 错误信息
     */
    public AiTimeoutException(String message) {
        super(message);
    }

    /**
     * 构造超时异常（带原因）。
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public AiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建默认的超时异常。
     *
     * @return AiTimeoutException 实例
     */
    public static AiTimeoutException defaultInstance() {
        return new AiTimeoutException("AI 服务响应超时，请稍后重试");
    }

    /**
     * 创建带超时时长的超时异常。
     *
     * @param timeoutSeconds 超时秒数
     * @return AiTimeoutException 实例
     */
    public static AiTimeoutException withTimeout(int timeoutSeconds) {
        return new AiTimeoutException(
                String.format("AI 服务响应超时（超过 %d 秒），请稍后重试", timeoutSeconds));
    }
}
