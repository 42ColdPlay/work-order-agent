package com.agent.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG Pipeline —— 检索增强生成核心流程。
 * <p>
 * 完整流程：
 * <pre>
 * 用户问题
 *   → Query Rewrite（改写问题以提升检索效果）
 *   → Embedding + 向量检索
 *   → Re-ranking（重排序）
 *   → 上下文组装（送入 LLM）
 * </pre>
 * <p>
 * 当前版本实现了基础链路，后续可扩展：
 * - HyDE（先让 LLM 生成假设答案，再用假设答案检索）
 * - Multi-Query（生成多个查询维度，合并检索结果）
 * - RAG Fusion（融合多路召回结果）
 */
@Service
public class RAGPipeline {

    private static final Logger log = LoggerFactory.getLogger(RAGPipeline.class);

    private final VectorStoreService vectorStoreService;

    private static final int DEFAULT_TOP_K = 5;

    public RAGPipeline(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 检索增强：根据用户问题检索知识库，返回拼接后的上下文文本。
     *
     * @param query 用户问题
     * @return 格式化后的上下文（可直接注入 Prompt）
     */
    public String retrieve(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }

        // Step 1: 向量检索
        List<Document> docs = vectorStoreService.search(query, DEFAULT_TOP_K);

        if (docs.isEmpty()) {
            log.debug("RAG 未检索到相关内容: query=\"{}\"", query);
            return "";
        }

        // Step 2: 拼接上下文
        String context = docs.stream()
                .map(doc -> String.format("【来源: %s】%n%s",
                        doc.getMetadata().getOrDefault("source", "未知"),
                        doc.getText()))
                .collect(Collectors.joining("\n\n---\n\n"));

        log.info("RAG 检索成功: query=\"{}\", 命中 {} 条文档", query, docs.size());
        return context;
    }

    /**
     * 带 HyDE 的检索（扩展预留）。
     * HyDE: 先让 LLM 生成一个假设的理想答案，再用这个答案去向量库检索，
     * 可以提升召回率（即使 query 和文档的措辞不同也能命中）。
     */
    public String retrieveWithHyDE(String query, String hypotheticalAnswer) {
        // HyDE 使用假设答案作为检索 query，而不是原始问题
        String hydeQuery = hypotheticalAnswer != null && !hypotheticalAnswer.isBlank()
                ? query + "\n" + hypotheticalAnswer
                : query;
        return retrieve(hydeQuery);
    }
}
