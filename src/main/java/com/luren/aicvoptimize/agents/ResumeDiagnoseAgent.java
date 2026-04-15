package com.luren.aicvoptimize.agents;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.luren.aicvoptimize.dto.CvDiagnoseResponse;
import com.luren.aicvoptimize.prompt.PromptKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 简历诊断智能体
 * <p>
 * 根据简历内容和目标职位，诊断简历匹配度并给出优化建议。
 *
 * @author lijianpan
 */
@Component
@RequiredArgsConstructor
public class ResumeDiagnoseAgent {

    private static final String NAME = "resume_diagnosis_agent";
    private static final String OUTPUT_KEY = "diagnosis_result";

    private final AgentFactory agentFactory;

    /**
     * 创建简历诊断智能体
     *
     * @param variables 模板变量，需包含 resume_text、target_job
     * @return ReactAgent 实例
     */
    public ReactAgent create(Map<String, Object> variables) {
        return agentFactory.createAgent(NAME, PromptKey.RESUME_DIAGNOSIS_AGENT,
                variables, CvDiagnoseResponse.class, OUTPUT_KEY);
    }
}
