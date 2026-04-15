package com.luren.aicvoptimize.dto;

import lombok.Data;

import java.util.List;

/**
 * 简历优化（改写）响应（结构化 JSON）。
 * <p>
 * 输出包含：优化后的简历全文、关键改动对照、岗位定制关键词与待补充问题。
 */
@Data
public class CvOptimizeResponse {
    /**
     * 优化后的简历全文（markdown 或 text，取决于请求 outputFormat）。
     * <p>
     * 注意：必须遵守“不编造事实”的约束，所有信息应来源于用户原始简历或其等价改写。
     */
    private String optimizedResume;

    /**
     * 关键改动对照（建议 6-12 条），便于用户审阅与二次编辑。
     */
    private List<Change> changes;

    /**
     * 为该岗位提取/建议覆盖的关键词（来源于 JD），用于提升 ATS 与匹配度。
     */
    private List<String> tailoredKeywords;

    /**
     * 为进一步提升效果需要补充的信息问题清单（最多 10 条更合适）。
     */
    private List<String> clarificationQuestions;

    /**
     * 预留字段：调试或上游透传用。
     * <p>
     * 当前约定固定输出空字符串。
     */
    private String rawModelText;

    @Data
    public static class Change {
        /**
         * 变更发生的简历章节，如：个人总结/项目经历/工作经历/技能等。
         */
        private String section;

        /**
         * 原表述（允许为从原文抽取的片段）。
         */
        private String before;

        /**
         * 改写后的表述（不新增事实，仅优化表达/结构/量化方式）。
         */
        private String after;

        /**
         * 变更原因（例如：更 ATS 友好、更匹配岗位、更量化、更清晰）。
         */
        private String rationale;
    }
}

