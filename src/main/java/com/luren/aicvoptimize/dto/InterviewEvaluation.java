package com.luren.aicvoptimize.dto;

import lombok.Data;

import java.util.List;

/**
 * 面试回答评估DTO
 * <p>
 * 用于评估用户的面试回答
 *
 * @author lijianpan
 */
@Data
public class InterviewEvaluation {
    /**
     * 问题ID
     */
    private String questionId;

    /**
     * 用户回答
     */
    private String userAnswer;

    /**
     * 综合评分（0-100）
     */
    private int score;

    /**
     * 评分维度详情
     */
    private ScoreDimensions dimensions;

    /**
     * 回答优点
     */
    private List<String> strengths;

    /**
     * 改进建议
     */
    private List<String> improvements;

    /**
     * 示例优质回答
     */
    private String sampleGoodAnswer;

    /**
     * 是否建议追问
     */
    private boolean suggestFollowUp;

    /**
     * 追问问题（如果有）
     */
    private String followUpQuestion;

    @Data
    public static class ScoreDimensions {
        /**
         * 逻辑性评分（0-100）
         */
        private int logic;

        /**
         * 完整性评分（0-100）
         */
        private int completeness;

        /**
         * 专业性评分（0-100）
         */
        private int professionalism;

        /**
         * 表达能力评分（0-100）
         */
        private int expression;

        /**
         * 与职位匹配度评分（0-100）
         */
        private int relevance;
    }
}
