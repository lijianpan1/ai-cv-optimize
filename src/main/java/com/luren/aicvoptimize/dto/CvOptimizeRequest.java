package com.luren.aicvoptimize.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 简历优化（改写）请求。
 * <p>
 * 与诊断相比，此接口会产出“优化后的简历全文”，因此建议提供尽可能完整的 JD 与简历信息。
 */
@Data
public class CvOptimizeRequest {
    /**
     * 简历全文（纯文本）。
     * <p>
     * 必填；模型会在“不编造事实”的前提下进行改写与重组。
     */
    @NotBlank
    @Size(max = 120000)
    private String resumeText;

    /**
     * 目标岗位标题（可选），用于引导优化方向与措辞风格。
     */
    @Size(max = 200)
    private String targetJobTitle;

    /**
     * 目标岗位描述 JD（可选）。
     * <p>
     * 用于提取 tailoredKeywords，并在优化简历中自然覆盖（不硬塞不存在的技能）。
     */
    @Size(max = 120000)
    private String targetJobDescription;

    /**
     * 年限/级别（可选），如：1-3年/3-5年/高级/应届。
     */
    @Size(max = 50)
    private String seniority;

    /**
     * 输出语言偏好（可选），推荐：zh-CN / en-US。
     */
    @Size(max = 20)
    private String language;

    /**
     * 优化后简历的文本格式（可选）。
     * <p>
     * 推荐：markdown / text。默认 markdown。
     */
    @Size(max = 40)
    private String outputFormat;
}

