package com.agent.memory.entity;

/**
 * 长期记忆状态。
 * <p>
 * 不做物理删除：被新事实取代的旧记忆标记为 SUPERSEDED，
 * 既保留审计痕迹，也避免"用户换部门后新旧事实同时命中"的自相矛盾。
 */
public enum MemoryStatus {
    ACTIVE,
    SUPERSEDED
}
