# OnlineShopAI API 文档

## 基础信息

- **Base URL**: `http://localhost:8081`
- **Content-Type**: `application/json`
- **编码**: UTF-8

---

## 1. 同步聊天

### 请求

```http
POST /api/chat
Content-Type: application/json
```

### 请求体

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | string | 否 | 会话ID，默认自动生成 |
| userId | string | 否 | 用户ID |
| question | string | ✅ | 用户问题 |
| timestamp | number | 否 | 时间戳 |

### 请求示例

```json
{
  "sessionId": "user-123-session-001",
  "userId": "user-001",
  "question": "你好，请介绍一下这个电商平台",
  "timestamp": 1743443200000
}
```

### 响应示例

```json
{
  "sessionId": "user-123-session-001",
  "question": "你好，请介绍一下这个电商平台",
  "response": "欢迎来到我们的电商平台！我们提供...",
  "duration": 1250,
  "timestamp": 1743443201500
}
```

---

## 2. 流式聊天 (SSE)

### 请求

```http
POST /api/chat/stream
Content-Type: application/json
```

### 请求体

同「同步聊天」

### 请求示例

```json
{
  "sessionId": "user-123-session-001",
  "question": "推荐一些电子产品",
  "timestamp": 1743443200000
}
```

### 响应 (Server-Sent Events)

```text
data:欢
data:迎
data:来
data:到
data:...
data:[DONE]
```

### React 前端对接示例

```tsx
import { useState } from 'react';

function ChatComponent() {
  const [message, setMessage] = useState('');
  const [response, setResponse] = useState('');

  const handleStreamChat = async () => {
    const response = await fetch('http://localhost:8081/api/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: 'session-001',
        userId: 'user-001',
        question: message
      })
    });

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();

    if (reader) {
      let fullResponse = '';
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value);
        if (chunk !== '[DONE]') {
          fullResponse += chunk;
          setResponse(fullResponse);
        }
      }
    }
  };

  return (
    <div>
      <input value={message} onChange={(e) => setMessage(e.target.value)} />
      <button onClick={handleStreamChat}>发送</button>
      <div>{response}</div>
    </div>
  );
}
```

---

## 3. GET 快速测试

```http
GET /api/chat?q=你好
```

### 响应示例

```json
{
  "question": "你好",
  "response": "你好！有什么可以帮助你的吗？",
  "duration": 800,
  "timestamp": 1743443200000
}
```

---

## 4. 健康检查

```http
GET /api/health
```

### 响应示例

```json
{
  "status": "UP",
  "service": "onlineShopAI",
  "timestamp": 1743443200000
}
```

---

## 错误响应

```json
{
  "sessionId": "user-123-session-001",
  "error": "抱歉，AI服务暂时繁忙，请稍后重试。",
  "timestamp": 1743443200000
}
```

---

## React Hook 封装示例

```tsx
// useChat.ts
import { useState, useCallback } from 'react';

interface ChatOptions {
  sessionId?: string;
  userId?: string;
}

export function useChat(options: ChatOptions = {}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const sendMessage = useCallback(async (question: string): Promise<string> => {
    setLoading(true);
    setError(null);

    try {
      const res = await fetch('http://localhost:8081/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sessionId: options.sessionId || `session-${Date.now()}`,
          userId: options.userId || 'anonymous',
          question,
          timestamp: Date.now()
        })
      });

      const data = await res.json();

      if (data.error) {
        throw new Error(data.error);
      }

      return data.response;
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unknown error');
      throw e;
    } finally {
      setLoading(false);
    }
  }, [options.sessionId, options.userId]);

  const streamMessage = useCallback(async (
    question: string,
    onChunk: (text: string) => void
  ): Promise<void> => {
    setLoading(true);
    setError(null);

    const res = await fetch('http://localhost:8081/api/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: options.sessionId || `session-${Date.now()}`,
        userId: options.userId || 'anonymous',
        question,
        timestamp: Date.now()
      })
    });

    const reader = res.body?.getReader();
    const decoder = new TextDecoder();

    if (!reader) {
      throw new Error('No reader available');
    }

    let fullResponse = '';
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value);
      if (chunk !== '[DONE]') {
        fullResponse += chunk;
        onChunk(fullResponse);
      }
    }

    setLoading(false);
  }, [options.sessionId, options.userId]);

  return { sendMessage, streamMessage, loading, error };
}
```

---

## WebSocket 对接

### 连接地址

```
ws://localhost:8081/ws/chat
```

### 消息格式

**客户端发送：**
```json
{
  "type": "chat",
  "sessionId": "user-123-session-001",
  "userId": "user-001",
  "question": "推荐一些商品"
}
```

**服务端响应：**
```json
{
  "type": "chunk",
  "content": "欢迎",
  "sessionId": "user-123-session-001"
}
```

```json
{
  "type": "done",
  "fullContent": "欢迎来到我们的电商平台...",
  "sessionId": "user-123-session-001"
}
```

### React WebSocket 示例

```tsx
function WebSocketChat() {
  const [ws, setWs] = useState<WebSocket | null>(null);
  const [messages, setMessages] = useState<string[]>([]);

  const connect = () => {
    const socket = new WebSocket('ws://localhost:8081/ws/chat');

    socket.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.type === 'chunk') {
        setMessages(prev => [...prev, data.content]);
      } else if (data.type === 'done') {
        console.log('Complete response:', data.fullContent);
      }
    };

    setWs(socket);
  };

  const sendMessage = (question: string) => {
    ws?.send(JSON.stringify({
      type: 'chat',
      sessionId: `session-${Date.now()}`,
      question
    }));
  };

  return (
    <div>
      <button onClick={connect}>连接</button>
      <button onClick={() => sendMessage('你好')}>发送</button>
      <div>{messages.join('')}</div>
    </div>
  );
}
```
