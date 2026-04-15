package com.luren.aicvoptimize.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 简历文本请求（纯文本模式工作流输入）
 */
@Data
public class ResumeTextRequest {

    @NotBlank(message = "简历文本不能为空")
    private String resumeText;

    private String resumeName;
}
