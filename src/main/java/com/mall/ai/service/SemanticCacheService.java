package com.mall.ai.service;

/**
 * 语义缓存服务接口。
 * 
 * <p>基于 Redis Vector Store 实现语义级别的缓存复用。
 * 通过向量相似度匹配，将语义相近的问题映射到相同的缓存响应。</p>
 * 
 * <p>工作原理：</p>
 * <ol>
 *   <li>将用户问题通过 Embedding 模型转换为向量</li>
 *   <li>在 Redis Vector Store 中检索相似向量</li>
 *   <li>若相似度超过阈值（默认 0.92），返回缓存的响应</li>
 *   <li>否则返回 null，表示缓存未命中</li>
 * </ol>
 * 
 * <p>优势：</p>
 * <ul>
 *   <li>减少大模型 API 调用次数，降低成本</li>
 *   <li>提升响应速度，改善用户体验</li>
 *   <li>支持语义级别的匹配，而非精确匹配</li>
 * </ul>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
public interface SemanticCacheService {

    /**
     * 查找相似问题的缓存响应。
     * 
     * <p>将问题转换为向量，在 Redis Vector Store 中检索相似问题，
     * 若相似度超过阈值则返回对应的缓存响应。</p>
     *
     * @param question 用户提问内容
     * @return 命中时返回缓存的响应，否则返回 null
     * @throws IllegalArgumentException 当 question 为 null 或空白时
     */
    String findSimilarResponse(String question);

    /**
     * 保存问答对到语义缓存。
     * 
     * <p>将问题和响应存储到 Redis Vector Store，
     * 问题的向量嵌入用于后续的相似度检索。</p>
     *
     * @param question 用户提问内容
     * @param response AI 回复内容
     * @throws IllegalArgumentException 当 question 或 response 为 null 或空白时
     */
    void saveResponse(String question, String response);

    /**
     * 清除所有语义缓存。
     * 
     * <p>用于缓存管理或数据重置场景。</p>
     */
    void clearAllCache();

    /**
     * 获取缓存统计信息。
     *
     * @return 缓存条目数量
     */
    long getCacheCount();
}
