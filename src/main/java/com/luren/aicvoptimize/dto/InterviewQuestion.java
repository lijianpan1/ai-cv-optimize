package com.luren.aicvoptimize.dto;

import lombok.Data;

import java.util.List;

/**
 * 面试问题DTO
 * <p>
 * 用于表示单个面试问题及其相关信息
 *
 * @author lijianpan
 */
@Data
public class InterviewQuestion {
    /**
     * 问题ID
     */
    private String questionId;

    /**
     * 问题内容
     */
    private String question;

    /**
     * 问题类型：technical（技术）、behavioral（行为）、situational（情境）、project（项目）
     */
    private String type;

    /**
     * 问题难度：easy、medium、hard
     */
    private String difficulty;

    /**
     * 问题关联的简历部分：workExperience、project、skill、education
     */
    private String relatedSection;

    /**
     * 考察要点列表
     */
    private List<String> evaluationPoints;

    /**
     * 参考答案/评分标准
     */
    private String referenceAnswer;

    /**
     * 问题权重（用于评分计算）
     */
    private int weight;
}
