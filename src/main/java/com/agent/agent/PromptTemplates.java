package com.agent.agent;

/**
 * Prompt 模板常量。
 * <p>
 * 后续可抽取到 resources/prompts/*.st 文件中，
 * 通过 Resource 加载，便于业务人员调整 Prompt 而无需改代码。
 */
public final class PromptTemplates {

    private PromptTemplates() {}

    /** 系统角色设定 */
    public static final String SYSTEM_PROMPT = """
            你是智能工单助手，可以处理工单相关的各种操作。

            核心能力：
            1. 查询工单状态、详情、进度
            2. 创建、分配、转派工单
            3. 审批流转、查询待办
            4. 统计报表、数据分析
            5. 根据知识库文档回答工单处理相关问题

            约束：
            - 如果用户要求执行操作（创建/审批/转派），先确认再执行
            - 如果知识库中找不到答案，如实告知，不要编造
            """;

    /** RAG 上下文注入模板 */
    public static final String RAG_TEMPLATE = """
            以下是知识库中可能与当前问题相关的参考信息：
            =====
            {context}
            =====
            请基于以上信息回答用户问题。如果参考信息不相关，忽略即可。
            """;
}
