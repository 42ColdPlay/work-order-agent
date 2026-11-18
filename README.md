# 智能工单助手 Agent

> 基于 Spring AI 搭建的企业级智能工单助手，通过自然语言驱动工单全生命周期管理。

---

## 技术栈

| 类别 | 技术 | 用途 |
|------|------|------|
| 框架 | Spring Boot 3.3 + Spring AI 1.0 | 应用骨架 + AI 编排 |
| 大模型 | DeepSeek API（主力）/ Qwen2.5:7b 本地（备选） | 自然语言理解与生成 |
| 工具调用 | Spring AI @Tool + OpenAI Function Calling | Agent 调用工单系统 API |
| RAG 知识库 | Chroma（向量库）+ nomic-embed-text（Embedding） | 知识检索增强 |
| 部署 | Docker Compose | 一键启动 |

## 系统架构

```
用户输入 → ChatClient → @Tool 调用（查/创/转/审）
                       → RAG 检索（知识库）
                       → LLM 推理 → 结构化输出 → 流式返回
```

## 快速启动

### 前置条件

- JDK 17+
- Docker Desktop（或 Colima）
- 可选：Ollama 本地已安装（已有则无需 Docker 中的 Ollama）

### 方式一：本地开发

```bash
# 1. 启动依赖服务
docker compose up chroma -d
# 如果本地没有 Ollama，也启动：
docker compose up ollama -d

# 2. 拉取模型（首次）
docker exec -it agent-ollama ollama pull qwen2.5:7b
docker exec -it agent-ollama ollama pull nomic-embed-text

# 3. 启动应用
# 使用 DeepSeek API：
export DEEPSEEK_API_KEY=sk-your-key
mvn spring-boot:run -Dspring-boot.run.profiles=deepseek

# 或使用本地 Ollama：
mvn spring-boot:run
```

### 方式二：Docker Compose 一键启动

```bash
docker compose up --build
```

## API 接口

### 对话

```bash
# 非流式
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"userId":"zhangsan","message":"我有哪些待办工单？"}'

# 流式 SSE
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"userId":"zhangsan","message":"创建一条高优先级的服务器宕机工单"}'
```

### 工单 REST API（模拟）

```bash
# 查询工单
curl http://localhost:8080/api/work-orders/WO-1

# 创建工单
curl -X POST http://localhost:8080/api/work-orders \
  -H "Content-Type: application/json" \
  -d '{"title":"测试工单","description":"测试","priority":"HIGH","reporterId":"zhangsan","assigneeId":"lisi"}'
```

## Agent 可用工具

| 工具 | 触发场景示例 |
|------|-------------|
| `getWorkOrderById` | "查一下 WO-1 的状态" |
| `getPendingWorkOrders` | "我有哪些待办工单？" |
| `createWorkOrder` | "创建一条服务器宕机工单" |
| `reassignWorkOrder` | "把 WO-1 转给李四处理" |
| `approveWorkOrder` | "同意 WO-1 的审批" |
| `rejectWorkOrder` | "驳回 WO-1，原因是不完整" |
| `getWorkOrderStatistics` | "统计一下各状态的工单数量" |

## 项目结构

```
src/main/java/com/agent/
├── AgentApplication.java       # 启动入口
├── config/
│   ├── AiModelConfig.java      # ChatClient 配置
│   ├── VectorStoreConfig.java  # 向量库配置
│   └── McpServerConfig.java    # Tool 注册
├── agent/
│   ├── WorkOrderAgent.java     # Agent 核心编排
│   └── PromptTemplates.java    # Prompt 模板
├── tool/
│   ├── WorkOrderToolService.java   # 工单查询/创建工具
│   ├── ApprovalToolService.java    # 审批流转工具
│   └── StatisticsToolService.java  # 统计报表工具
├── rag/
│   ├── DocumentService.java    # 文档切片
│   ├── VectorStoreService.java # 向量检索
│   └── RAGPipeline.java        # RAG 流程编排
├── wo/                         # 工单模拟系统
│   ├── entity/
│   ├── service/
│   └── controller/
└── web/
    └── ChatController.java     # 对话接口

src/main/resources/
├── application.yml             # 配置文件
├── prompts/                    # Prompt 模板文件
└── docs/                       # 知识库示例文档
```

## 扩展指南

### 切换模型

编辑 `application.yml`，注释/取消注释对应的配置块即可：

```yaml
# DeepSeek API
spring.ai.openai.api-key: ${DEEPSEEK_API_KEY}
spring.ai.openai.base-url: https://api.deepseek.com
spring.ai.openai.chat.options.model: deepseek-chat

# 本地 Qwen
spring.ai.ollama.base-url: http://localhost:11434
spring.ai.ollama.chat.options.model: qwen2.5:7b
```

### 添加新工具

1. 新建 Service 类
2. 添加 `@Tool` 注解方法（写好 description 和 @ToolParam）
3. Spring AI 自动注册，无需其他配置

### 更新知识库

将文档放入 `src/main/resources/docs/`，启动时自动加载存入向量库。
