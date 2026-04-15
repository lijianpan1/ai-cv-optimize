package com.luren.aicvoptimize.dto;

import lombok.Data;

import java.util.List;

/**
 * 面试会话DTO
 * <p>
 * 包含完整的面试题目库和会话状态
 *
 * @author lijianpan
 */
@Data
public class InterviewSession {
    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 目标职位名称
     */
    private String targetJobTitle;

    /**
     * 面试问题列表
     */
    private List<InterviewQuestion> questions;

    /**
     * 面试准备建议
     */
    private String preparationAdvice;

    /**
     * 候选人优势领域（简历亮点）
     */
    private List<String> strengthsToHighlight;

    /**
     * 需要准备的薄弱领域
     */
    private List<String> areasToImprove;

    /**
     * 预计面试时长（分钟）
     */
    private int estimatedDuration;
}
