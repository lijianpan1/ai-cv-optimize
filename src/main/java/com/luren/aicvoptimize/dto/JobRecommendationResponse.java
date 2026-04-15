package com.luren.aicvoptimize.dto;

import lombok.Data;
import lombok.Getter;

import java.util.List;

/**
 * 职位推荐响应（结构化 JSON）。
 * <p>
 * 包含候选人画像分析和基于简历的个性化职位推荐。
 */
@Data
public class JobRecommendationResponse {
    /**
     * 候选人画像分析
     */
    private CandidateProfile candidateProfile;

    /**
     * 推荐的职位列表（最多5个）
     */
    private List<RecommendedJob> recommendedJobs;

    /**
     * 职业发展建议或简历优化建议
     */
    private String careerAdvice;

    @Data
    public static class CandidateProfile {
        /**
         * 核心技能列表
         */
        private List<String> coreSkills;

        /**
         * 主要行业方向
         */
        private String industryFocus;

        /**
         * 主要职能方向
         */
        private String functionDirection;

        /**
         * 推断职级
         */
        private String seniorityLevel;

        /**
         * 推断工作年限或范围
         */
        private String yearsOfExperience;
    }

    @Data
    public static class RecommendedJob {
        /**
         * 推荐职位名称
         */
        private String jobTitle;

        /**
         * 匹配分数（0-100）
         */
        private int matchScore;

        /**
         * 匹配理由，强调技能与经历的契合点
         */
        private String reasoning;

        /**
         * 缺失的关键技能列表
         */
        private List<String> keyMissingSkills;
    }
}
