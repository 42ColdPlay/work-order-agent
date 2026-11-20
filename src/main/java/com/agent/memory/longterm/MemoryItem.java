package com.agent.memory.longterm;

/**
 * 一条可注入 prompt 的长期记忆。
 *
 * @param content   事实内容，独立成句
 * @param category  profile / preference / history / other
 * @param importance 1-10，越高越应该保留
 * @param createdAt ISO 时间字符串，展示时只取日期
 * @param score     召回相似度（列表查询时为 0）
 */
public record MemoryItem(String content, String category, int importance, String createdAt, double score) {
}
