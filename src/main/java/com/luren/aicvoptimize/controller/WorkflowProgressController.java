package com.luren.aicvoptimize.controller;

import com.luren.aicvoptimize.dto.ResumeTextRequest;
import com.luren.aicvoptimize.dto.WorkflowResultResponse;
import com.luren.aicvoptimize.dto.WorkflowState;
import com.luren.aicvoptimize.service.WorkflowProgressService;
import com.luren.aicvoptimize.workflow.StreamingWorkflowExecutor;
import com.luren.aicvoptimize.workflow.process.ResumeOptimizWorkflow;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 工作流进度实时推送控制器
 * <p>
 * 提供 SSE 接口用于实时推送工作流节点执行进度
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/progress")
@RequiredArgsConstructor
public class WorkflowProgressController {

    private final WorkflowProgressService progressService;
    private final StreamingWorkflowExecutor streamingExecutor;
    private final ResumeOptimizWorkflow workflow;

    /**
     * 建立 SSE 连接，用于接收实时进度
     *
     * @param request HTTP 请求
     * @return SseEmitter 实例
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(HttpServletRequest request) {
        // 生成唯一的客户端ID
        String clientId = UUID.randomUUID().toString();
        log.info("建立 SSE 连接，clientId: {}", clientId);
        return progressService.createEmitter(clientId);
    }

    /**
     * 交付式执行（基于文件上传）- 带实时进度推送
     * <p>
     * 先建立 SSE 连接获取 clientId，然后调用此接口
     *
     * @param file     简历文件
     * @param clientId 客户端ID（用于推送进度）
     * @return 工作流最终状态
     */
    @PostMapping(value = "/deliver", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkflowResultResponse> deliverWithProgress(
            @RequestPart("file") MultipartFile file,
            @RequestParam("clientId") String clientId) {
        log.info("交付式执行（带进度推送），clientId: {}", clientId);

        try {
            // 解析文件
            String resumeText = workflow.parseFileForText(file);
            String baseName = workflow.parseFileForBaseName(file);

            // 异步执行工作流并推送进度
            CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> result = workflow.deliverWithStreaming(resumeText, baseName, clientId);
                    // 发送完成事件
                    WorkflowState state = convertToWorkflowState(result);
                    progressService.sendComplete(clientId, state);
                } catch (Exception e) {
                    log.error("工作流执行失败", e);
                    progressService.sendError(clientId, e.getMessage());
                }
            });

            // 立即返回，实际结果通过 SSE 推送
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("启动工作流失败", e);
            progressService.sendError(clientId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 交付式执行（基于纯文本输入）- 带实时进度推送
     *
     * @param request  包含 resumeText 的请求体
     * @param clientId 客户端ID（用于推送进度）
     * @return 工作流最终状态
     */
    @PostMapping("/deliver/text")
    public ResponseEntity<WorkflowResultResponse> deliverWithProgressByText(
            @Valid @RequestBody ResumeTextRequest request,
            @RequestParam("clientId") String clientId) {
        log.info("交付式执行（文本模式，带进度推送），clientId: {}", clientId);

        // 异步执行工作流并推送进度
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> result = workflow.deliverWithStreaming(
                        request.getResumeText(), request.getResumeName(), clientId);
                // 发送完成事件
                WorkflowState state = convertToWorkflowState(result);
                progressService.sendComplete(clientId, state);
            } catch (Exception e) {
                log.error("工作流执行失败", e);
                progressService.sendError(clientId, e.getMessage());
            }
        });

        // 立即返回，实际结果通过 SSE 推送
        return ResponseEntity.accepted().build();
    }

    /**
     * 将 Map 转换为 WorkflowState
     */
    private WorkflowState convertToWorkflowState(Map<String, Object> result) {
        WorkflowState state = new WorkflowState();
        state.setResumeText((String) result.get("resume_text"));
        state.setResumeJson(result.get("resume_json"));
        state.setJobInfo(result.get("job_info"));
        state.setTargetJob(result.get("target_job"));
        state.setDiagnosisResult(result.get("diagnosis_result"));
        state.setOptimizedResumeJson(result.get("optimized_resume_json"));
        state.setAdversarialOptimizationResult(result.get("adversarial_optimization_result"));
        state.setOptimizedResume((String) result.get("optimized_resume"));
        state.setInterviewSession(result.get("interview_session"));
        state.setPdfPath((String) result.get("pdf_path"));
        state.setWordPath((String) result.get("word_path"));
        state.setPdfDownloadUrl((String) result.get("pdf_download_url"));
        state.setWordDownloadUrl((String) result.get("word_download_url"));
        return state;
    }
}
