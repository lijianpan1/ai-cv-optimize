package com.luren.aicvoptimize.dto;

import lombok.Data;

import java.util.List;

/**
 * 对抗性评估结果DTO
 * <p>
 * 包含红蓝军对抗评估的完整结果
 *
 * @author lijianpan
 */
@Data
public class AdversarialEvaluationResult {
    /**
     * 简历版本ID
     */
    private String versionId;

    /**
     * 综合评分（0-100）
     */
    private int overallScore;

    /**
     * HR视角评估（红军 - 挑剔视角）
     */
    private PerspectiveEvaluation hrPerspective;

    /**
     * 业务主管视角评估（蓝军 - 辩护视角）
     */
    private PerspectiveEvaluation managerPerspective;

    /**
     * 仲裁结果
     */
    private ArbitrationResult arbitrationResult;

    /**
     * 最终优化建议
     */
    private List<String> finalRecommendations;

    /**
     * 风险提示
     */
    private List<String> riskWarnings;

    /**
     * 是否通过质量审核
     */
    private boolean approved;

    @Data
    public static class PerspectiveEvaluation {
        /**
         * 评分（0-100）
         */
        private int score;

        /**
         * 优点列表
         */
        private List<String> pros;

        /**
         * 缺点列表
         */
        private List<String> cons;

        /**
         * 具体评论
         */
        private String commentary;

        /**
         * 攻击/辩护要点
         */
        private List<String> keyPoints;
    }

    @Data
    public static class ArbitrationResult {
        /**
         * 最终评分
         */
        private int finalScore;

        /**
         * 仲裁理由
         */
        private String reasoning;

        /**
         * 需要修改的要点
         */
        private List<String> requiredChanges;

        /**
         * 建议保留的亮点
         */
        private List<String> highlightsToKeep;

        /**
         * 风险等级：low、medium、high
         */
        private String riskLevel;
    }
}
