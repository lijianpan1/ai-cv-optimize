package com.luren.aicvoptimize.agents;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.luren.aicvoptimize.dto.ResumeParseResponse;
import com.luren.aicvoptimize.prompt.PromptKey;
import com.luren.aicvoptimize.rag.RAGDocumentWriter;
import com.luren.aicvoptimize.tools.DocumentSearchTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 简历优化智能体
 * <p>
 * 根据简历数据和职位描述，生成优化后的简历结构化数据。
 *
 * @author lijianpan
 */
@Component
@RequiredArgsConstructor
public class ResumeOptimizationAgent {

    private static final String NAME = "resume_optimization_agent";
    private static final String OUTPUT_KEY = "optimized_resume_json";

    private final AgentFactory agentFactory;
    private final RAGDocumentWriter ragDocumentWriter;

    /**
     * 创建简历优化智能体
     *
     * @param variables 模板变量，需包含 resume_data、job_description
     * @return ReactAgent 实例
     */
    public ReactAgent create(Map<String, Object> variables) {
        return agentFactory.createAgent(NAME, PromptKey.RESUME_OPTIMIZATION_AGENT,
                variables, ResumeParseResponse.class, OUTPUT_KEY);
    }
}
