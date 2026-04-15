package com.luren.aicvoptimize.dto;

import lombok.Data;

import java.util.List;

/**
 * 简历诊断响应（结构化 JSON）。
 * <p>
 * 说明：该 DTO 用于前端/调用方“稳定解析”，因此字段名尽量固定且语义明确。
 */
@Data
public class CvDiagnoseResponse {
    /**
     * 综合评分（0-100），用于快速衡量：ATS 友好度、岗位匹配度、表达清晰度与可信度。
     */
    private int overallScore;

    /**
     * 主要问题清单（按重要程度排序更佳）。
     */
    private List<Issue> issues;

    /**
     * 目标 JD 中出现，但简历未覆盖/未明确体现的关键词（可能是技能、领域词、职责关键词）。
     */
    private List<String> missingKeywords;

    /**
     * 简历已有亮点（用于保留与强化，不应在优化中被“抹掉”）。
     */
    private List<String> strengths;

    /**
     * 需要候选人补充的信息问题清单（用于二次迭代优化）。
     */
    private List<String> clarificationQuestions;

    /**
     * 预留字段：调试或上游透传用。
     * <p>
     * 当前约定固定输出空字符串，避免污染结构化内容。
     */
    private String rawModelText;

    @Data
    public static class Issue {
        /**
         * 问题所属区域/模块。
         * <p>
         * 示例：结构/内容/表达/量化/ATS/岗位匹配/项目描述/教育/技能/其他
         */
        private String area;

        /**
         * 严重程度：high / medium / low。
         */
        private String severity;

        /**
         * 具体问题描述（尽量客观、可验证）。
         */
        private String problem;

        /**
         * 可执行建议（尽量给“怎么改”的模板或方法）。
         */
        private String suggestion;
    }
}

