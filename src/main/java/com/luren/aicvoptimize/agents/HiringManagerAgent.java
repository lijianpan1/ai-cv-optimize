package com.luren.aicvoptimize.agents;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.luren.aicvoptimize.dto.AdversarialEvaluationResult;
import com.luren.aicvoptimize.prompt.PromptKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 业务主管辩护智能体（蓝军）
 * <p>
 * 模拟业务主管视角，辩护候选人的潜力，强调简历亮点和价值。
 * 在对抗性优化流程中扮演"蓝军"角色，负责维护和突出简历的优势。
 *
 * @author lijianpan
 */
@Component
@RequiredArgsConstructor
public class HiringManagerAgent {

    private static final String NAME = "hiring_manager_agent";
    private static final String OUTPUT_KEY = "manager_evaluation";

    private final AgentFactory agentFactory;

    /**
     * 创建业务主管辩护智能体
     *
     * @param variables 模板变量，需包含 resume_version、job_description
     * @return ReactAgent 实例
     */
    public ReactAgent create(Map<String, Object> variables) {
        return agentFactory.createAgent(NAME, PromptKey.HIRING_MANAGER_AGENT,
                variables, AdversarialEvaluationResult.PerspectiveEvaluation.class, OUTPUT_KEY);
    }
}
