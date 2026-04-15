package com.luren.aicvoptimize.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 面试预演请求DTO
 *
 * @author lijianpan
 */
@Data
public class InterviewRequest {
    /**
     * 会话ID（恢复面试时使用）
     */
    private String sessionId;

    /**
     * 优化后的简历JSON
     */
    @NotBlank(message = "简历数据不能为空")
    private String optimizedResumeJson;

    /**
     * 目标职位描述
     */
    @NotBlank(message = "目标职位描述不能为空")
    private String jobDescription;

    /**
     * 问题类型过滤器（可选）
     */
    private String questionType;

    /**
     * 问题数量限制
     */
    private int questionLimit = 5;
}
