package com.luren.aicvoptimize.agents;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.luren.aicvoptimize.dto.JobRecommendationResponse;
import com.luren.aicvoptimize.prompt.PromptKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 职位识别智能体
 * <p>
 * 根据简历结构化数据提取匹配的目标职位信息。
 *
 * @author lijianpan
 */
@Component
@RequiredArgsConstructor
public class JobDetectionAgent {

    private static final String NAME = "job_detection_agent";
    private static final String OUTPUT_KEY = "job_info";

    private final AgentFactory agentFactory;

    /**
     * 创建职位识别智能体
     *
     * @param variables 模板变量，需包含 resume_json
     * @return ReactAgent 实例
     */
    public ReactAgent create(Map<String, Object> variables) {
        return agentFactory.createAgent(NAME, PromptKey.JOB_DETECTION_AGENT,
                variables, JobRecommendationResponse.class, OUTPUT_KEY);
    }
}
