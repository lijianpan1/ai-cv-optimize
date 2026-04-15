package com.luren.aicvoptimize.service;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luren.aicvoptimize.agents.InterviewerAgent;
import com.luren.aicvoptimize.dto.InterviewEvaluation;
import com.luren.aicvoptimize.dto.InterviewSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 面试预演服务
 * <p>
 * 提供面试题库生成和回答评估功能
 *
 * @author lijianpan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewerAgent interviewerAgent;
    private final ObjectMapper objectMapper;

    /**
     * 会话缓存（生产环境应使用Redis等持久化存储）
     */
    private final Map<String, InterviewSession> sessionCache = new ConcurrentHashMap<>();

    /**
     * 生成面试题库
     *
     * @param optimizedResumeJson 优化后的简历JSON
     * @param jobDescription      职位描述
     * @param questionLimit       问题数量限制
     * @return 面试会话
     */
    public InterviewSession generateInterviewQuestions(
            String optimizedResumeJson, String jobDescription, int questionLimit) throws Exception {
        
        log.info("生成面试题库，简历长度: {}, 职位描述长度: {}", 
                optimizedResumeJson.length(), jobDescription.length());

        Map<String, Object> variables = Map.of(
                "optimized_resume", optimizedResumeJson,
                "job_description", jobDescription
        );

        ReactAgent agent = interviewerAgent.createForQuestionGeneration(variables);
        AssistantMessage message = agent.call("请根据简历和职位描述生成" + questionLimit + "道面试题目");
        
        InterviewSession session = objectMapper.readValue(message.getText(), InterviewSession.class);
        session.setSessionId(UUID.randomUUID().toString());
        
        // 缓存会话
        sessionCache.put(session.getSessionId(), session);
        
        log.info("面试题库生成完成，会话ID: {}, 题目数量: {}", 
                session.getSessionId(), session.getQuestions() != null ? session.getQuestions().size() : 0);
        
        return session;
    }

    /**
     * 评估用户回答
     *
     * @param sessionId     会话ID
     * @param questionId    问题ID
     * @param answer        用户回答
     * @param jobDescription 职位描述
     * @return 评估结果
     */
    public InterviewEvaluation evaluateAnswer(
            String sessionId, String questionId, String answer, String jobDescription) throws Exception {
        
        log.info("评估回答，会话ID: {}, 问题ID: {}", sessionId, questionId);

        // 获取会话
        InterviewSession session = sessionCache.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在或已过期: " + sessionId);
        }

        // 查找问题
        InterviewEvaluation evaluation = findQuestionAndEvaluate(session, questionId, answer, jobDescription);
        
        log.info("回答评估完成，评分: {}", evaluation.getScore());
        
        return evaluation;
    }

    /**
     * 获取缓存的会话
     */
    public InterviewSession getSession(String sessionId) {
        return sessionCache.get(sessionId);
    }

    /**
     * 清除会话缓存
     */
    public void clearSession(String sessionId) {
        sessionCache.remove(sessionId);
        log.info("会话已清除: {}", sessionId);
    }

    /**
     * 查找问题并评估回答
     */
    private InterviewEvaluation findQuestionAndEvaluate(
            InterviewSession session, String questionId, String answer, String jobDescription) throws Exception {
        
        var question = session.getQuestions().stream()
                .filter(q -> q.getQuestionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("问题不存在: " + questionId));

        Map<String, Object> variables = Map.of(
                "question", objectMapper.writeValueAsString(question),
                "user_answer", answer,
                "job_description", jobDescription
        );

        ReactAgent agent = interviewerAgent.createForEvaluation(variables);
        AssistantMessage message = agent.call("请评估候选人的回答");
        
        InterviewEvaluation evaluation = objectMapper.readValue(message.getText(), InterviewEvaluation.class);
        evaluation.setQuestionId(questionId);
        evaluation.setUserAnswer(answer);
        
        return evaluation;
    }
}
