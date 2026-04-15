package com.luren.aicvoptimize.dto;

import lombok.Data;

import java.util.List;

/**
 * 简历优化版本DTO
 * <p>
 * 用于对抗性优化流程中管理多个优化版本
 *
 * @author lijianpan
 */
@Data
public class ResumeOptimizationVersion {
    /**
     * 版本ID
     */
    private String versionId;

    /**
     * 版本标签：conservative（保守）、balanced（平衡）、aggressive（进取）
     */
    private String versionLabel;

    /**
     * 优化策略描述
     */
    private String strategyDescription;

    /**
     * 优化后的简历结构化数据
     */
    private ResumeParseResponse optimizedResume;

    /**
     * 主要修改点
     */
    private List<Modification> modifications;

    /**
     * 评估结果
     */
    private AdversarialEvaluationResult evaluationResult;

    /**
     * 是否为最终选中版本
     */
    private boolean isSelected;

    @Data
    public static class Modification {
        /**
         * 修改区域：workExperience、project、skill、education等
         */
        private String section;

        /**
         * 修改前内容
         */
        private String before;

        /**
         * 修改后内容
         */
        private String after;

        /**
         * 修改原因
         */
        private String rationale;
    }
}
