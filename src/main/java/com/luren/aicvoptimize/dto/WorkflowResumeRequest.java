package com.luren.aicvoptimize.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 工作流恢复请求（交互式模式 - 用户选择职位后恢复）
 */
@Data
public class WorkflowResumeRequest {

    /**
     * 会话 ID（由 start 接口返回的 state 中获取，用于恢复中断的工作流）
     */
    @NotBlank(message = "会话ID不能为空")
    private String threadId;

    /**
     * 用户选择的目标职位 JSON，传 null 表示使用默认最高分职位
     */
    private String targetJob;
}
