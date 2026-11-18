package com.agent.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析与切片服务。
 * <p>
 * RAG 的第一步：将原始文档（PDF/Word/TXT）解析为文本，然后按策略切分为 Chunk。
 * <p>
 * 切片策略：
 * - 固定长度切片（1000 tokens）+ 重叠窗口（200 tokens）
 * - 保留段落边界（避免在段落中间切断）
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    // 切片参数
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    /**
     * 从文本创建文档切片。
     * 生产环境需要对接 PDFBox（PDF）和 POI（Word）做格式解析。
     */
    public List<Document> chunkText(String sourceName, String text) {
        List<Document> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) continue;

            if (currentChunk.length() + trimmed.length() > CHUNK_SIZE && currentChunk.length() > 0) {
                chunks.add(buildDocument(sourceName, currentChunk.toString()));
                // 重叠：保留最后 CHUNK_OVERLAP 字符
                String overlap = currentChunk.length() > CHUNK_OVERLAP
                        ? currentChunk.substring(currentChunk.length() - CHUNK_OVERLAP)
                        : currentChunk.toString();
                currentChunk = new StringBuilder(overlap);
            }
            currentChunk.append(trimmed).append("\n");
        }
        if (currentChunk.length() > 0) {
            chunks.add(buildDocument(sourceName, currentChunk.toString()));
        }

        log.info("文档 [{}] 切分为 {} 个切片", sourceName, chunks.size());
        return chunks;
    }

    private Document buildDocument(String sourceName, String content) {
        Document doc = new Document(content);
        doc.getMetadata().put("source", sourceName);
        return doc;
    }

    /**
     * 从 classpath 加载文档资源。
     */
    public String loadClasspathDoc(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                log.warn("文档未找到: {}", path);
                return "";
            }
            return new String(is.readAllBytes());
        } catch (Exception e) {
            log.error("加载文档失败: {}", path, e);
            return "";
        }
    }
}
