package com.agent.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量库操作服务。
 * <p>
 * 封装 VectorStore 的读写操作：
 * - 写入：将 Chunk 转为向量存入 Chroma
 * - 查询：将问题转为向量，在 Chroma 中检索 Top-K 相似文档
 */
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final VectorStore vectorStore;
    private final double similarityThreshold;

    public VectorStoreService(VectorStore vectorStore,
                              @Value("${rag.similarity-threshold:0.5}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * 将文档切片存入向量库。
     *
     * @param documents 文档切片列表
     * @param namespace 命名空间（用于区分不同知识库）
     */
    public void store(List<Document> documents, String namespace) {
        documents.forEach(doc -> doc.getMetadata().put("namespace", namespace));
        vectorStore.add(documents);
        log.info("已存入 {} 个文档切片到命名空间 [{}]", documents.size(), namespace);
    }

    /**
     * 检索与查询最相似的 Top-K 文档。
     *
     * @param query  用户问题
     * @param topK   返回数量
     * @return 相似文档列表
     */
    public List<Document> search(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);
        log.debug("向量检索: query=\"{}\", topK={}, 命中={} 条", query, topK, results.size());
        return results;
    }

    /**
     * 删除命名空间下的所有文档（用于重新索引）。
     */
    public void deleteByNamespace(String namespace) {
        Filter.Expression filter = new FilterExpressionBuilder().eq("namespace", namespace).build();
        vectorStore.delete(filter);
        log.info("已清除命名空间 [{}] 下的所有文档", namespace);
    }
}
