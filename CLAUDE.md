# CLAUDE.md
所有回答使用简体中文，包括思考过程
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands
```bash
# Build (skip tests)
mvn clean package -DskipTests

# Run with default profile (Ollama local)
mvn spring-boot:run

# Run with DeepSeek API
export DEEPSEEK_API_KEY=sk-your-key
mvn spring-boot:run -Dspring-boot.run.profiles=deepseek

# Run specific test class
mvn test -Dtest=TestClassName

# Run single test method
mvn test -Dtest=TestClassName#testMethod

# Docker Compose (full stack)
docker compose up --build
```

## Project Architecture

Spring AI Agent 应用，核心链路：

```
用户 → ChatController → WorkOrderAgent (ChatClient)
                          ├── System Prompt (角色设定)
                          ├── MessageChatMemoryAdvisor (20轮对话记忆)
                          ├── @Tool 调用 (Function Calling)
                          │   ├── WorkOrderToolService  (查/创/转派)
                          │   ├── ApprovalToolService   (审批/驳回)
                          │   └── StatisticsToolService (统计)
                          ├── RAGPipeline
                          │   ├── DocumentService    (文本切片)
                          │   ├── VectorStoreService (Chroma 检索)
                          │   └── RAGPipeline        (上下文组装)
                          └── LLM 推理 → 结构化输出
```

### 目录结构

- `agent/` — Agent 核心编排 (`WorkOrderAgent`) + Prompt 模板
- `tool/` — @Tool 工具类，每个方法是一个 Function Calling 工具
- `rag/` — RAG 流程：文档加载 → 切片 → 向量化 → 检索
- `wo/` — 工单模拟系统 (entity/service/controller)，内存 ConcurrentHashMap
- `config/` — AiModelConfig (ChatClient)、McpServerConfig (Tool注册)、VectorStoreConfig
- `web/` — ChatController (POST /api/chat, /api/chat/stream SSE)

### 关键设计决策

- **双模型支持**：默认使用 Ollama 本地 Qwen2.5:7b，配置 `spring.ai.openai.*` 可切换 DeepSeek API (profile=deepseek)
- **Tool 自动注册**：`McpServerConfig` 中 `ToolCallbacks.from()` 扫描所有 @Tool 方法，无需手动注册
- **RAG 流程**：启动时从 `resources/docs/` 加载文档 → 切片 → Embedding → 存入 Chroma；查询时向量检索 Top-5
- **工单系统模拟**：`WorkOrderMockService` 用 `ConcurrentHashMap` 内存模拟，含预置示例数据 (@PostConstruct)
- **对话记忆**：基于 `MessageChatMemoryAdvisor` + `InMemoryChatMemory`，按 userId 隔离对话窗口
- **无测试**：目前项目没有任何测试代码