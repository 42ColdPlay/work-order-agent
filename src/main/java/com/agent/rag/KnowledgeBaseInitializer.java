package com.agent.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库启动初始化器。
 * <p>
 * 应用启动后把 classpath 下的 SOP 文档切片并写入向量库，
 * 否则 RAG 链路虽然接通、但知识库为空，检索永远返回空结果。
 * <p>
 * 设计取舍：
 * - 用 ApplicationRunner 而不是 @PostConstruct：此时所有 Bean（含 VectorStore）已就绪
 * - 整体 try-catch：向量库不可用时不阻塞应用启动，RAG 降级为空而不是整个服务起不来
 * - 切片使用确定性 ID：重复启动会覆盖而非产生重复数据
 */
@Component
public class KnowledgeBaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseInitializer.class);

    private static final String NAMESPACE = "work-order-sop";
    private static final String SOP_PATH = "docs/sample-sop.md";

    private final DocumentService documentService;
    private final VectorStoreService vectorStoreService;

    public KnowledgeBaseInitializer(DocumentService documentService,
                                    VectorStoreService vectorStoreService) {
        this.documentService = documentService;
        this.vectorStoreService = vectorStoreService;
    }

    @Override
    public void run(ApplicationArguments args) {
        long start = System.currentTimeMillis();
        try {
            String text = documentService.loadClasspathDoc(SOP_PATH);
            if (text.isBlank()) {
                log.warn("知识库文档为空或未找到：{}，RAG 将不可用", SOP_PATH);
                return;
            }

            List<Document> chunks = documentService.chunkText(SOP_PATH, text);
            vectorStoreService.store(chunks, NAMESPACE);

            log.info("知识库初始化完成：{} -> {} 个切片，命名空间 [{}]，耗时 {}ms",
                    SOP_PATH, chunks.size(), NAMESPACE, System.currentTimeMillis() - start);
        } catch (Exception e) {
            // 向量库不可用（如 Chroma 未启动）不应导致应用启动失败，RAG 降级为空即可
            log.error("知识库初始化失败，RAG 将降级为空知识库（不影响工单操作等其他功能）: {}", e.getMessage());
        }
    }
}
