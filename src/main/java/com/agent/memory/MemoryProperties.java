package com.agent.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 记忆模块配置。
 * <p>
 * 分两层：
 * - chat：会话级记忆（短期），对应 Redis 缓存 + MySQL 流水
 * - longterm：跨会话的长期记忆（抽取式），对应 MySQL 记忆表 + 向量库
 */
@Component
@ConfigurationProperties(prefix = "memory")
public class MemoryProperties {

    private final Chat chat = new Chat();
    private final LongTerm longterm = new LongTerm();

    public Chat getChat() {
        return chat;
    }

    public LongTerm getLongterm() {
        return longterm;
    }

    public static class Chat {

        /** Redis key 过期时间，过期后由 MySQL 流水重建缓存 */
        private Duration ttl = Duration.ofDays(7);

        /** Redis 缓存与单次重建的最大消息条数 */
        private int maxMessages = 100;

        /** 注入模型的 token 预算，超出则从最旧的消息开始丢弃 */
        private int tokenBudget = 3000;

        /** 是否把消息流水持久化到 MySQL（关闭后仅保留 Redis 窗口） */
        private boolean persist = true;

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public int getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
        }

        public int getTokenBudget() {
            return tokenBudget;
        }

        public void setTokenBudget(int tokenBudget) {
            this.tokenBudget = tokenBudget;
        }

        public boolean isPersist() {
            return persist;
        }

        public void setPersist(boolean persist) {
            this.persist = persist;
        }
    }

    public static class LongTerm {

        /** 长期记忆总开关 */
        private boolean enabled = true;

        /** 是否在每轮对话后异步抽取记忆 */
        private boolean extractionEnabled = true;

        /** 召回注入 prompt 的条数 */
        private int topK = 5;

        /** 召回相似度阈值 */
        private double recallThreshold = 0.45;

        /** 达到该相似度视为"同一主题"，旧记忆会被新记忆取代 */
        private double conflictThreshold = 0.80;

        /** 达到该相似度视为完全重复，直接跳过 */
        private double duplicateThreshold = 0.93;

        /** 单轮最多沉淀的记忆条数 */
        private int maxFactsPerTurn = 5;

        /** 用户消息短于该长度时跳过抽取（大概率是"好的""嗯"这类无信息量内容） */
        private int minMessageLength = 8;

        /** 抽取线程池大小 */
        private int executorPoolSize = 2;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isExtractionEnabled() {
            return extractionEnabled;
        }

        public void setExtractionEnabled(boolean extractionEnabled) {
            this.extractionEnabled = extractionEnabled;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getRecallThreshold() {
            return recallThreshold;
        }

        public void setRecallThreshold(double recallThreshold) {
            this.recallThreshold = recallThreshold;
        }

        public double getConflictThreshold() {
            return conflictThreshold;
        }

        public void setConflictThreshold(double conflictThreshold) {
            this.conflictThreshold = conflictThreshold;
        }

        public double getDuplicateThreshold() {
            return duplicateThreshold;
        }

        public void setDuplicateThreshold(double duplicateThreshold) {
            this.duplicateThreshold = duplicateThreshold;
        }

        public int getMaxFactsPerTurn() {
            return maxFactsPerTurn;
        }

        public void setMaxFactsPerTurn(int maxFactsPerTurn) {
            this.maxFactsPerTurn = maxFactsPerTurn;
        }

        public int getMinMessageLength() {
            return minMessageLength;
        }

        public void setMinMessageLength(int minMessageLength) {
            this.minMessageLength = minMessageLength;
        }

        public int getExecutorPoolSize() {
            return executorPoolSize;
        }

        public void setExecutorPoolSize(int executorPoolSize) {
            this.executorPoolSize = executorPoolSize;
        }
    }
}
