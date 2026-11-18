package com.agent.config;

import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量库配置 —— 初始化 Chroma 并写入示例知识数据。
 * <p>
 * RAG 流程：文档 → 切片 → Embedding → 存入 VectorStore
 * 查询时：问题 → Embedding → 向量检索 → 返回 Top-K 相似文档
 */
@Configuration
public class VectorStoreConfig {

    /**
     * 向量检索的助手方法，后续在 RAGPipeline 中注入使用。
     * 这里仅做 Schema 展示，实际数据写入由 DocumentService 处理。
     */
    @Bean
    public FilterExpressionBuilder filterExpressionBuilder() {
        return new FilterExpressionBuilder();
    }
}
