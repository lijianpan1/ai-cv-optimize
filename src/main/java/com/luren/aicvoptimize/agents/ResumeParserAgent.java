package com.luren.aicvoptimize.agents;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.luren.aicvoptimize.dto.ResumeParseResponse;
import com.luren.aicvoptimize.prompt.PromptKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 简历解析智能体
 * <p>
 * 将原始简历文本解析为结构化 JSON 数据。
 *
 * @author lijianpan
 */
@Component
@RequiredArgsConstructor
public class ResumeParserAgent {

    private static final String NAME = "resume_parser_agent";
    private static final String OUTPUT_KEY = "resume_json";

    private final AgentFactory agentFactory;

    /**
     * 创建简历解析智能体
     *
     * @param variables 模板变量，需包含 resume_text
     * @return ReactAgent 实例
     */
    public ReactAgent create(Map<String, Object> variables) {
        return agentFactory.createAgent(NAME, PromptKey.RESUME_PARSER_AGENT,
                variables, ResumeParseResponse.class, OUTPUT_KEY);
    }
}
