package com.luren.aicvoptimize.controller;

import com.luren.aicvoptimize.dto.InterviewAnswerRequest;
import com.luren.aicvoptimize.dto.InterviewEvaluation;
import com.luren.aicvoptimize.dto.InterviewRequest;
import com.luren.aicvoptimize.dto.InterviewSession;
import com.luren.aicvoptimize.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 面试预演 REST API
 * <p>
 * 提供面试题库生成和回答评估接口
 *
 * @author lijianpan
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewService interviewService;

    /**
     * 生成面试题库
     * <p>
     * 根据优化后的简历和职位描述生成针对性面试题目
     *
     * @param request 面试请求
     * @return 面试会话（包含题目列表）
     */
    @PostMapping("/generate")
    public InterviewSession generateQuestions(@Valid @RequestBody InterviewRequest request) {
        log.info("收到面试题库生成请求");
        try {
            return interviewService.generateInterviewQuestions(
                    request.getOptimizedResumeJson(),
                    request.getJobDescription(),
                    request.getQuestionLimit()
            );
        } catch (Exception e) {
            log.error("生成面试题库失败", e);
            throw new RuntimeException("生成面试题库失败: " + e.getMessage(), e);
        }
    }

    /**
     * 提交回答并获取评估
     * <p>
     * 评估用户对特定问题的回答，返回多维度评分和改进建议
     *
     * @param request 回答请求
     * @return 评估结果
     */
    @PostMapping("/evaluate")
    public InterviewEvaluation evaluateAnswer(@Valid @RequestBody InterviewAnswerRequest request) {
        log.info("收到回答评估请求，会话ID: {}, 问题ID: {}", request.getSessionId(), request.getQuestionId());
        try {
            // 从会话中获取职位描述
            InterviewSession session = interviewService.getSession(request.getSessionId());
            if (session == null) {
                throw new IllegalArgumentException("会话不存在或已过期");
            }
            
            return interviewService.evaluateAnswer(
                    request.getSessionId(),
                    request.getQuestionId(),
                    request.getAnswer(),
                    session.getTargetJobTitle()
            );
        } catch (Exception e) {
            log.error("评估回答失败", e);
            throw new RuntimeException("评估回答失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取面试会话
     *
     * @param sessionId 会话ID
     * @return 面试会话
     */
    @GetMapping("/session/{sessionId}")
    public InterviewSession getSession(@PathVariable String sessionId) {
        InterviewSession session = interviewService.getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在或已过期: " + sessionId);
        }
        return session;
    }

    /**
     * 清除面试会话
     *
     * @param sessionId 会话ID
     */
    @DeleteMapping("/session/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        interviewService.clearSession(sessionId);
    }
}
