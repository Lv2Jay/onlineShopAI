package com.mall.ai.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

/**
 * 商品文档向量模型。
 * 
 * <p>用于存储商品信息及其向量嵌入表示，
 * 支持基于 Redis Vector Store 的语义检索。</p>
 * 
 * <p>该实体存储于 Redis Hash 结构中，Key 格式为 {@code doc:productId}。</p>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "doc", timeToLive = 86400)
public class ProductDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品唯一标识。
     */
    @Id
    private String productId;

    /**
     * 商品名称。
     */
    private String name;

    /**
     * 商品描述。
     * 用于生成向量嵌入的主要文本内容。
     */
    private String description;

    /**
     * 商品分类。
     */
    private String category;

    /**
     * 商品价格（单位：分）。
     */
    private Long priceInCents;

    /**
     * 商品标签列表。
     * JSON 数组格式存储。
     */
    private String tags;

    /**
     * 向量嵌入表示。
     * 由 Embedding 模型生成的浮点数组。
     */
    private float[] embedding;

    /**
     * 扩展元数据。
     * 存储额外的商品属性信息。
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 创建时间戳（毫秒）。
     */
    private Long createdAt;

    /**
     * 更新时间戳（毫秒）。
     */
    private Long updatedAt;

    /**
     * 添加元数据字段。
     *
     * @param key   元数据键
     * @param value 元数据值
     * @return 当前实例（支持链式调用）
     */
    public ProductDocument addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
}
