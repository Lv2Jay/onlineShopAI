# OnlineShopAI - AI 电商客服微服务

基于 Spring Boot 3.4 + Spring AI 1.0.0-M6 构建的智能电商客服系统，集成 DeepSeek 大模型与 Redis 向量数据库，提供语义理解、智能问答、流式响应等能力。

## ✨ 核心特性

- 🤖 **AI 智能对话** - 集成 DeepSeek 大模型，支持自然语言理解的客服问答
- ⚡ **流式响应** - WebSocket + SSE 双模式，打字机效果实时推送
- 🧠 **语义缓存** - 基于 Redis Vector Store 的语义级别缓存，降低 API 调用成本
- 🔌 **多模型支持** - 支持 Ollama 本地Embedding 模型（shaw/dmeta-embedding-zh）
- 📦 **商品语义检索** - 支持商品描述的向量化存储与语义搜索
- 🛡️ **生产级质量** - 全局异常处理、参数校验、优雅降级

## 🏗️ 技术架构

### 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.4.4 | 微服务框架 |
| Spring AI | 1.0.0-M6 | AI 工程化框架 |
| JDK | 21 | Java 21 虚拟线程 |
| Redis Stack | 7.2+ | 向量数据库（启用 RedisSearch 模块） |
| Ollama | Latest | 本地 Embedding 模型部署 |
| DeepSeek | API | 大语言模型 |

### 核心依赖

```xml
<dependencies>
    <!-- Spring AI 核心 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        <version>1.0.0-M6</version>
    </dependency>
    
    <!-- Ollama Embedding -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-ollama</artifactId>
        <version>1.0.0-M6</version>
    </dependency>
    
    <!-- Redis 向量存储 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-redis-store</artifactId>
        <version>1.0.0-M6</version>
    </dependency>
    
    <!-- WebSocket 流式响应 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
</dependencies>
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- Redis Stack 7.2+（启用 RedisSearch 模块）
- Ollama（运行本地 Embedding 模型）
- DeepSeek API Key

### 配置说明

#### 1. application.yml 核心配置

```yaml
spring:
  ai:
    # DeepSeek API 配置（通过 OpenAI 兼容模式）
    openai:
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY}
      chat:
        options:
          model: deepseek-chat
      
      # Ollama Embedding 模型配置
    ollama:
      base-url: http://localhost:11434
      embedding:
        options:
          model: shaw/dmeta-embedding-zh
    
    # Redis 向量存储配置
    vectorstore:
      redis:
        index-name: ai-semantic-cache
        prefix: doc:
  
  data:
    redis:
      host: localhost
      port: 6379

# AI 服务自定义配置
ai:
  deepseek:
    timeout-seconds: 60
    fallback-message: 抱歉，AI 服务暂时繁忙，请稍后重试。
  
  semantic-cache:
    enabled: true
    similarity-threshold: 0.92  # 语义相似度阈值
    ttl-seconds: 3600           # 缓存过期时间（秒）
```

#### 2. 环境变量

创建 `.env` 文件（项目根目录）：

```bash
DEEPSEEK_API_KEY=your_deepseek_api_key_here
```

#### 3. 启动 Ollama 并下载 Embedding 模型

```bash
# 启动 Ollama 服务
ollama serve

# 下载中文 Embedding 模型
ollama pull shaw/dmeta-embedding-zh
```

#### 4. 启动应用

```bash
# 编译
mvn clean install

# 运行
mvn spring-boot:run

# 或直接运行 jar
java -jar target/onlineShopAI-1.0.0-SNAPSHOT.jar
```

应用启动后访问：`http://localhost:8080`

## 📡 API 接口

### REST API

#### 1. 同步聊天接口

```bash
POST /api/chat
Content-Type: application/json

{
  "sessionId": "session-123",
  "userId": "user-456",
  "question": "这款手机的续航能力如何？"
}
```

#### 2. 流式聊天接口（SSE）

```bash
POST /api/chat/stream
Content-Type: application/json
Accept: text/event-stream

{
  "sessionId": "session-123",
  "userId": "user-456",
  "question": "介绍一下草莓"
}
```

#### 3. 快速测试接口

```bash
GET /api/chat?q=你好
```

#### 4. 清除语义缓存

```bash
POST /api/cache/clear
```

#### 5. 查看缓存统计

```bash
GET /api/cache/stats
```

#### 6. 健康检查

```bash
GET /api/health
```

### WebSocket API

#### 连接端点

```
ws://localhost:8080/ws/ai/chat
```

#### 请求消息格式

```json
{
  "sessionId": "session-123",
  "userId": "user-456",
  "question": "这款手机支持 5G 吗？"
}
```

#### 响应消息格式

**Token 流式推送：**
```json
{
  "sessionId": "session-123",
  "type": "TOKEN",
  "content": "这",
  "timestamp": 1712345678901,
  "fromCache": false
}
```

**完成标识：**
```json
{
  "sessionId": "session-123",
  "type": "COMPLETE",
  "timestamp": 1712345678902
}
```

**缓存命中：**
```json
{
  "sessionId": "session-123",
  "type": "CACHED",
  "content": "完整的回答内容",
  "timestamp": 1712345678903,
  "fromCache": true
}
```

**错误响应：**
```json
{
  "sessionId": "session-123",
  "type": "ERROR",
  "errorCode": "AI_ERROR",
  "errorMessage": "AI 服务暂时不可用",
  "timestamp": 1712345678904
}
```

## 🧠 语义缓存机制

### 工作原理

1. **问题向量化** - 用户提问通过 Ollama Embedding 模型转换为向量
2. **相似度检索** - 在 Redis Vector Store 中检索相似向量
3. **缓存匹配** - 相似度 > 0.92 时返回缓存响应
4. **AI 调用** - 未命中时调用 DeepSeek API，并存入缓存

### 缓存管理

```bash
# 清除所有缓存
curl -X POST http://localhost:8080/api/cache/clear

# 查看缓存条目数
curl http://localhost:8080/api/cache/stats
```

### 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `ai.semantic-cache.enabled` | true | 是否启用语义缓存 |
| `ai.semantic-cache.similarity-threshold` | 0.92 | 语义相似度阈值（0-1） |
| `ai.semantic-cache.ttl-seconds` | 3600 | 缓存过期时间（秒） |

## 📂 项目结构

```
onlineShopAI/
├── src/main/java/com/mall/ai/
│   ├── config/                    # 配置类
│   │   ├── DeepSeekConfig.java    # DeepSeek 配置
│   │   ├── VectorStoreConfig.java # Redis 向量存储配置
│   │   └── WebSocketConfig.java   # WebSocket 配置
│   ├── controller/                # 控制器
│   │   ├── ChatController.java    # REST API
│   │   └── AiWebSocketHandler.java # WebSocket 处理器
│   ├── service/                   # 服务层
│   │   ├── AiChatService.java     # AI 聊天服务接口
│   │   ├── SemanticCacheService.java # 语义缓存接口
│   │   └── impl/                  # 实现类
│   ├── model/                     # 数据模型
│   │   ├── ChatRequest.java       # 请求模型
│   │   ├── ChatResponse.java      # 响应模型
│   │   └── ProductDocument.java   # 商品文档
│   ├── exception/                 # 异常处理
│   │   ├── BusinessException.java # 业务异常
│   │   └── GlobalExceptionHandler.java # 全局异常处理器
│   └── OnlineShopAiApplication.java # 启动类
├── src/main/resources/
│   ├── application.yml            # 主配置文件
│   └── .env                       # 环境变量（不提交到 Git）
├── pom.xml                        # Maven 配置
└── README.md                      # 项目文档
```

## 🔧 开发指南

### 添加新的 Embedding 模型

修改 `application.yml`：

```yaml
spring:
  ai:
    ollama:
      embedding:
        options:
          model: your-model-name
```

### 自定义 Prompt

在 Service 层构建 Prompt：

```java
String prompt = """
你是一名专业的电商客服助手，请用友好、专业的语气回答用户问题。

用户问题：%s
""".formatted(question);
```

### 扩展商品语义检索

1. 定义商品文档模型：

```java
@RedisHash(value = "doc:product", timeToLive = 86400)
public class ProductDocument {
    @Id
    private String productId;
    private String name;
    private String description;
    // ...
}
```

2. 批量导入商品：

```java
List<Document> documents = products.stream()
    .map(p -> new Document(
        p.getDescription(),
        Map.of("productId", p.getId(), "name", p.getName())
    ))
    .toList();

vectorStore.add(documents);
```

## 🛡️ 生产部署建议

### 1. Redis 高可用

- 使用 Redis Cluster 或 Sentinel
- 配置持久化策略（RDB+AOF）
- 设置合理的内存淘汰策略

### 2. API 限流

```yaml
ai:
  deepseek:
    rate-limit:
      enabled: true
      requests-per-minute: 60
```

### 3. 监控告警

集成 Spring Boot Actuator + Prometheus + Grafana：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 4. 日志优化

```yaml
logging:
  level:
    com.mall.ai: INFO
    org.springframework.ai: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## 🐛 常见问题

### Q1: 语义缓存命中率低？

**A:** 调低相似度阈值：
```yaml
ai.semantic-cache.similarity-threshold: 0.85
```

### Q2: WebSocket 连接失败？

**A:** 检查跨域配置和防火墙设置，确认端口 8080 可访问。

### Q3: Ollama 模型下载失败？

**A:** 使用国内镜像或手动下载模型文件。

### Q4: Redis Vector Store 初始化失败？

**A:** 确认 Redis Stack 7.2+ 且启用了 RedisSearch 模块：
```bash
redis-cli MODULE LIST | grep search
```

## 📊 性能优化

### 1. 向量维度选择

使用轻量级 Embedding 模型（如 bge-m3）降低计算开销。

### 2. 缓存预热

系统启动时预加载高频问题：

```java
@Component
public class CacheWarmer implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        // 预加载常见问题
    }
}
```

### 3. 异步处理

使用虚拟线程（Java 21）提升并发性能：

```yaml
spring:
  threads:
    virtual:
      enabled: true
```



## 📧 联系方式

如有问题请提交 Issue 或通过邮箱联系。

---

**Built with ❤️ using Spring Boot 3.4 + Spring AI 1.0.0-M6**
