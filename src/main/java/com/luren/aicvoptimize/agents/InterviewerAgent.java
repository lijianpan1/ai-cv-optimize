package com.luren.aicvoptimize.agents;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.luren.aicvoptimize.dto.InterviewSession;
import com.luren.aicvoptimize.prompt.PromptKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 面试官智能体
 * <p>
 * 根据优化后的简历和目标职位JD，生成高针对性的面试题库。
 * 支持多轮对话，能针对用户的回答进行追问，并根据回答的逻辑性、完整度进行实时评分。
 *
 * @author lijianpan
 */
@Component
@RequiredArgsConstructor
public class InterviewerAgent {

    private static final String NAME = "interviewer_agent";
    private static final String OUTPUT_KEY = "interview_session";

    private final AgentFactory agentFactory;

    /**
     * 创建面试官智能体（生成面试题库）
     *
     * @param variables 模板变量，需包含 optimized_resume、job_description
     * @return ReactAgent 实例
     */
    public ReactAgent createForQuestionGeneration(Map<String, Object> variables) {
        return agentFactory.createAgent(NAME, PromptKey.INTERVIEWER_AGENT,
                variables, InterviewSession.class, OUTPUT_KEY);
    }

    /**
     * 创建面试官智能体（评估回答）
     *
     * @param variables 模板变量，需包含 question、user_answer、job_description
     * @return ReactAgent 实例
     */
    public ReactAgent createForEvaluation(Map<String, Object> variables) {
        return agentFactory.createAgent(NAME + "_evaluator", PromptKey.INTERVIEWER_EVALUATION_AGENT,
                variables, InterviewSession.class, "interview_evaluation");
    }
}
