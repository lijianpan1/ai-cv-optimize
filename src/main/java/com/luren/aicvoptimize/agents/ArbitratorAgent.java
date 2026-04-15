package com.luren.aicvoptimize.agents;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.luren.aicvoptimize.dto.AdversarialEvaluationResult;
import com.luren.aicvoptimize.prompt.PromptKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 仲裁智能体
 * <p>
 * 作为"质量把关人"，综合HR视角和业务主管视角的对抗结果，
 * 进行最终仲裁，给出优化建议和风险提示。
 *
 * @author lijianpan
 */
@Component
@RequiredArgsConstructor
public class ArbitratorAgent {

    private static final String NAME = "arbitrator_agent";
    private static final String OUTPUT_KEY = "arbitration_result";

    private final AgentFactory agentFactory;

    /**
     * 创建仲裁智能体
     *
     * @param variables 模板变量，需包含 resume_version、job_description、
     *                  hr_evaluation、manager_evaluation
     * @return ReactAgent 实例
     */
    public ReactAgent create(Map<String, Object> variables) {
        return agentFactory.createAgent(NAME, PromptKey.ARBITRATOR_AGENT,
                variables, AdversarialEvaluationResult.ArbitrationResult.class, OUTPUT_KEY);
    }
}
