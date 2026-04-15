package com.luren.aicvoptimize.controller;

import com.luren.aicvoptimize.dto.ResumeTextRequest;
import com.luren.aicvoptimize.dto.WorkflowResultResponse;
import com.luren.aicvoptimize.dto.WorkflowResumeRequest;
import com.luren.aicvoptimize.exception.QuotaExceededException;
import com.luren.aicvoptimize.service.UserQuotaService;
import com.luren.aicvoptimize.workflow.process.ResumeOptimizWorkflow;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 简历优化工作流 REST API
 * <p>
 * 提供三种执行模式：
 * <ul>
 *   <li><b>一步执行</b>（run）：带持久化、有默认中断点但不暂停，直接跑完</li>
 *   <li><b>交互式执行</b>（start + resume）：在职位选择节点中断，用户选择后恢复</li>
 *   <li><b>交付式执行</b>（deliver）：无中断、无持久化，一键跑完，最轻量</li>
 * </ul>
 *
 * @author lijianpan
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/workflow/resume")
public class ResumeWorkflowController {

    private final ResumeOptimizWorkflow workflow;
    private final UserQuotaService userQuotaService;

    // ==================== 交互式执行（中断-恢复模式） ====================

    /**
     * 启动交互式工作流（基于文件上传）
     * <p>
     * 工作流执行到职位选择节点时暂停，返回当前状态（含职位推荐列表）。
     * 前端展示推荐职位后，通过 {@link #resume(WorkflowResumeRequest)} 恢复执行。
     *
     * @param file 简历文件
     * @return 工作流执行结果（interrupted=true 表示已暂停等待用户选择）
     */
    @PostMapping(value = "/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public WorkflowResultResponse start(@RequestPart("file") MultipartFile file) {
        // 检查使用次数
        if (!userQuotaService.hasRemainingQuota()) {
            log.warn("用户尝试启动工作流但次数已用完");
            throw new QuotaExceededException();
        }
        ResumeOptimizWorkflow.StartResult result = workflow.start(file);
        return WorkflowResultResponse.from(result.workflowResult(), result.threadId());
    }

    /**
     * 启动交互式工作流（基于纯文本输入）
     *
     * @param request 包含 resumeText 字段的请求体
     * @return 工作流执行结果（interrupted=true 表示已暂停等待用户选择）
     */
    @PostMapping("/start/text")
    public WorkflowResultResponse startByText(@Valid @RequestBody ResumeTextRequest request) {
        // 检查使用次数
        if (!userQuotaService.hasRemainingQuota()) {
            log.warn("用户尝试启动工作流但次数已用完");
            throw new QuotaExceededException();
        }
        ResumeOptimizWorkflow.StartResult result = workflow.start(request.getResumeText(), request.getResumeName());
        return WorkflowResultResponse.from(result.workflowResult(), result.threadId());
    }

    /**
     * 恢复交互式工作流（用户选择目标职位后调用）
     * <p>
     * 将用户选择的职位注入工作流状态，从断点恢复执行剩余节点（简历诊断 → 简历优化）。
     * <p>
     * 注意：使用次数在 start 时已检查，此处执行成功后会减少次数
     *
     * @param request 包含 threadId 和可选 targetJob 的请求体
     * @return 工作流执行结果（interrupted=false 表示已全部完成）
     */
    @PostMapping("/resume")
    public WorkflowResultResponse resume(@Valid @RequestBody WorkflowResumeRequest request) {
        WorkflowResultResponse result = WorkflowResultResponse.from(
                workflow.resumeWithJobSelection(request.getThreadId(), request.getTargetJob())
        );
        // 执行成功且未中断（全部完成），减少使用次数
        if (!result.isInterrupted()) {
            int remaining = userQuotaService.decrementQuota();
            log.info("交互式工作流完成，剩余次数: {}", remaining);
        }
        return result;
    }

    // ==================== 交付式执行（一键完成，无需交互） ====================

    /**
     * 交付式执行（基于文件上传）
     * <p>
     * 无中断、无持久化，工作流从文件解析到简历优化一键跑完。
     * 目标职位由系统自动选择匹配度最高的职位。
     *
     * @param file 简历文件
     * @return 工作流最终状态（含简历诊断、优化结果）
     */
    @PostMapping(value = "/deliver", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> deliver(@RequestPart("file") MultipartFile file) {
        // 检查使用次数
        if (!userQuotaService.hasRemainingQuota()) {
            log.warn("用户尝试交付工作流但次数已用完");
            throw new QuotaExceededException();
        }
        Map<String, Object> result = workflow.deliver(file);
        // 执行成功，减少使用次数
        int remaining = userQuotaService.decrementQuota();
        log.info("交付式工作流完成，剩余次数: {}", remaining);
        return result;
    }

    /**
     * 交付式执行（基于纯文本输入）
     *
     * @param request 包含 resumeText 字段的请求体
     * @return 工作流最终状态（含简历诊断、优化结果）
     */
    @PostMapping("/deliver/text")
    public Map<String, Object> deliverByText(@Valid @RequestBody ResumeTextRequest request) {
        // 检查使用次数
        if (!userQuotaService.hasRemainingQuota()) {
            log.warn("用户尝试交付工作流但次数已用完");
            throw new QuotaExceededException();
        }
        Map<String, Object> result = workflow.deliver(request.getResumeText(), request.getResumeName());
        // 执行成功，减少使用次数
        int remaining = userQuotaService.decrementQuota();
        log.info("交付式工作流完成，剩余次数: {}", remaining);
        return result;
    }
}
