package com.mall.ai.config;

import com.mall.ai.controller.AiWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类。
 * 
 * <p>配置 WebSocket 端点与跨域访问策略，
 * 支持 Web 前端的 WebSocket 连接。</p>
 * 
 * <p>端点配置：</p>
 * <ul>
 *   <li>路径: {@code /ws/ai/chat}</li>
 *   <li>处理器: {@link AiWebSocketHandler}</li>
 *   <li>跨域: 允许所有来源（生产环境应配置具体域名）</li>
 * </ul>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AiWebSocketHandler aiWebSocketHandler;

    /**
     * 注册 WebSocket 处理器。
     * 
     * <p>将 AI 聊天处理器注册到指定路径，
     * 配置允许的跨域来源。</p>
     *
     * @param registry WebSocket 处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(aiWebSocketHandler, "/ws/ai/chat")
                .setAllowedOrigins("*");
    }
}
