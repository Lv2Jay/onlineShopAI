package com.mall.ai.model;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 聊天请求模型。
 * 
 * <p>封装前端通过 WebSocket 发送的用户提问请求，
 * 包含会话标识、用户标识及问题内容。</p>
 * 
 * <p>JSON 示例：</p>
 * <pre>{@code
 * {
 *   "sessionId": "session-123",
 *   "userId": "user-456",
 *   "question": "这款手机的续航能力如何？"
 * }
 * }</pre>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话唯一标识。
     * 用于关联同一对话上下文中的多轮问答。
     */
    private String sessionId;

    /**
     * 用户唯一标识。
     * 用于用户级别的个性化配置与权限控制。
     */
    private String userId;

    /**
     * 用户提问内容。
     * 必填字段，不允许为空或纯空白字符。
     */
    @NotBlank(message = "问题内容不能为空")
    private String question;

    /**
     * 请求时间戳（毫秒）。
     * 可选字段，未设置时由系统自动填充当前时间。
     */
    private Long timestamp;
}
