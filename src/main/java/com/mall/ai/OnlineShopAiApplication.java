package com.mall.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OnlineShopAI 微服务主启动类。
 * 
 * <p>基于 Spring Boot 3.4.x 构建，启用 Java 21 虚拟线程特性，
 * 集成 Spring AI 与 DeepSeek 大模型，提供智能对话服务。</p>
 * 
 * <p>核心特性：</p>
 * <ul>
 *   <li>虚拟线程并发：通过 {@code spring.threads.virtual.enabled=true} 启用</li>
 *   <li>WebSocket 流式响应：支持打字机效果的前端展示</li>
 *   <li>语义缓存：基于 Redis Vector Store 实现相似问题缓存复用</li>
 * </ul>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 * @since Java 21
 */
@SpringBootApplication
public class OnlineShopAiApplication {

    /**
     * 应用程序入口方法。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(OnlineShopAiApplication.class, args);
    }
}
