package com.mall.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 向量存储配置类。
 * 
 * <p>配置 Redis 作为 Vector Store，用于语义缓存的向量检索。
 * Redis Stack 提供原生的向量索引支持，适合中小规模的语义检索场景。</p>
 * 
 * <p>功能职责：</p>
 * <ul>
 *   <li>语义缓存：存储问题-响应的向量嵌入</li>
 *   <li>商品检索：存储商品描述的向量嵌入</li>
 * </ul>
 * 
 * <p>Redis Stack 要求：</p>
 * <ul>
 *   <li>Redis 版本 >= 7.2</li>
 *   <li>启用 RedisSearch 模块</li>
 * </ul>
 * 
 * <p>注意：VectorStore 由 Spring AI Redis Starter 自动配置，
 * 通过 application.yml 中的 spring.ai.vectorstore.redis.* 属性配置。</p>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
@Configuration
public class RedisVectorConfig {

    /**
     * 配置 RedisTemplate。
     * 
     * <p>使用 String 序列化器作为 Key 序列化器，
     * JSON 序列化器作为 Value 序列化器。</p>
     *
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
