package com.luren.aicvoptimize.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 简历诊断请求。
 * <p>
 * 说明：
 * - 以纯文本为输入，调用方可直接从 PDF/Word 复制粘贴
 * - 设置长度上限是为了避免超长文本导致模型调用成本/延迟不可控
 */
@Data
public class CvDiagnoseRequest {
    /**
     * 简历全文（纯文本）。
     * <p>
     * 必填；建议包含：个人简介/技能/工作经历/项目经历/教育等。
     */
    @NotBlank
    @Size(max = 120000)
    private String resumeText;

    /**
     * 目标岗位标题（可选），如：Java后端开发工程师。
     * <p>
     * 用于引导模型做岗位匹配分析与关键词缺口识别。
     */
    @Size(max = 200)
    private String targetJobTitle;

    /**
     * 目标岗位描述 JD（可选）。
     * <p>
     * 建议直接粘贴招聘描述正文；越完整，匹配度分析越准确。
     */
    @Size(max = 120000)
    private String targetJobDescription;

    /**
     * 年限/级别（可选），如：1-3年/3-5年/高级/应届。
     * <p>
     * 用于控制建议的“深度与侧重点”（例如更强调基础能力 vs 架构与影响力）。
     */
    @Size(max = 50)
    private String seniority;

    /**
     * 输出语言偏好（可选）。
     * <p>
     * 推荐值：zh-CN / en-US。
     */
    @Size(max = 20)
    private String language;
}

