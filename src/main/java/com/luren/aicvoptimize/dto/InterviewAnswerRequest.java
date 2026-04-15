package com.luren.aicvoptimize.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 面试回答提交请求DTO
 *
 * @author lijianpan
 */
@Data
public class InterviewAnswerRequest {
    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 问题ID
     */
    @NotBlank(message = "问题ID不能为空")
    private String questionId;

    /**
     * 用户回答
     */
    @NotBlank(message = "回答内容不能为空")
    private String answer;
}
