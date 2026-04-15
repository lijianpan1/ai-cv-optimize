package com.luren.aicvoptimize.agents;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.luren.aicvoptimize.dto.AdversarialEvaluationResult;
import com.luren.aicvoptimize.prompt.PromptKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 挑剔HR评估智能体（红军）
 * <p>
 * 模拟挑剔的HR视角，攻击简历中的弱点，识别潜在风险和问题。
 * 在对抗性优化流程中扮演"红军"角色，负责找出简历的不足之处。
 *
 * @author lijianpan
 */
@Component
@RequiredArgsConstructor
public class CriticalHrAgent {

    private static final String NAME = "critical_hr_agent";
    private static final String OUTPUT_KEY = "hr_evaluation";

    private final AgentFactory agentFactory;

    /**
     * 创建挑剔HR评估智能体
     *
     * @param variables 模板变量，需包含 resume_version、job_description
     * @return ReactAgent 实例
     */
    public ReactAgent create(Map<String, Object> variables) {
        return agentFactory.createAgent(NAME, PromptKey.CRITICAL_HR_AGENT,
                variables, AdversarialEvaluationResult.PerspectiveEvaluation.class, OUTPUT_KEY);
    }
}
