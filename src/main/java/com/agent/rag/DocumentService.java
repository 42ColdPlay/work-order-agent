package com.agent.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档解析与切片服务。
 * <p>
 * RAG 的第一步：将原始文档（PDF/Word/TXT）解析为文本，然后按策略切分为 Chunk。
 * <p>
 * 切片策略：
 * - 按空行切段落，累计到 CHUNK_SIZE 字符切一刀
 * - 保留 CHUNK_OVERLAP 字符的重叠窗口，避免语义被切断
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    // 切片参数
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    /**
     * 从文本创建文档切片。
     * <p>
     * 每个切片使用「源文件 + 序号」生成确定性 ID，
     * 这样重复加载同一份文档时会覆盖而不是产生重复数据。
     */
    public List<Document> chunkText(String sourceName, String text) {
        List<Document> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) continue;

            if (currentChunk.length() + trimmed.length() > CHUNK_SIZE && currentChunk.length() > 0) {
                chunks.add(buildDocument(sourceName, currentChunk.toString(), chunks.size()));
                // 重叠：保留最后 CHUNK_OVERLAP 字符
                String overlap = currentChunk.length() > CHUNK_OVERLAP
                        ? currentChunk.substring(currentChunk.length() - CHUNK_OVERLAP)
                        : currentChunk.toString();
                currentChunk = new StringBuilder(overlap);
            }
            currentChunk.append(trimmed).append("\n");
        }
        if (currentChunk.length() > 0) {
            chunks.add(buildDocument(sourceName, currentChunk.toString(), chunks.size()));
        }

        log.info("文档 [{}] 切分为 {} 个切片", sourceName, chunks.size());
        return chunks;
    }

    private Document buildDocument(String sourceName, String content, int index) {
        String id = UUID.nameUUIDFromBytes(
                (sourceName + "#" + index).getBytes(StandardCharsets.UTF_8)).toString();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", sourceName);
        metadata.put("chunkIndex", index);

        return new Document(id, content, metadata);
    }

    /**
     * 从 classpath 加载文档资源。
     * <p>
     * 显式指定 UTF-8：避免在非 UTF-8 默认字符集的机器上中文乱码。
     */
    public String loadClasspathDoc(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                log.warn("文档未找到: {}", path);
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("加载文档失败: {}", path, e);
            return "";
        }
    }
}
