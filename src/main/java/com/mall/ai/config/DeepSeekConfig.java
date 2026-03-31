package com.mall.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek AI 配置类。
 * 
 * <p>配置 Spring AI 的 ChatClient，用于与 DeepSeek 大模型交互。
 * 通过 Spring AI 的 OpenAI 兼容模式接入 DeepSeek API。</p>
 * 
 * <p>配置要点：</p>
 * <ul>
 *   <li>使用 application.yml 中的 base-url 和 api-key 配置</li>
 *   <li>ChatClient.Builder 由 Spring AI 自动配置注入</li>
 *   <li>支持流式响应和同步调用两种模式</li>
 * </ul>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
@Configuration
public class DeepSeekConfig {

    /**
     * 创建 ChatClient Bean。
     * 
     * <p>ChatClient.Builder 已由 Spring AI 自动配置，
     * 此处仅作为显式 Bean 定义便于后续扩展配置。</p>
     *
     * @param builder ChatClient 构建器，由 Spring AI 自动注入
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
