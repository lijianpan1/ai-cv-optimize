package com.luren.aicvoptimize.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.luren.aicvoptimize.agents.ResumeParserAgent;
import com.luren.aicvoptimize.util.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 简历解析节点
 * <p>
 * 接收原始简历文本，通过简历解析智能体解析为结构化 JSON 数据
 *
 * @author lijianpan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeParserNode implements NodeAction {

    private final ResumeParserAgent resumeParserAgent;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("=== 开始执行：简历解析节点 ===");

        String resumeText = (String) state.value("resume_text")
                .orElseThrow(() -> new IllegalArgumentException("缺少 resume_text 输入"));

        ReactAgent agent = resumeParserAgent.create(Map.of("resume_text", resumeText));
        AssistantMessage message = agent.call("请生成一个结构化JSON数据");
        String result = StrUtils.markdownToJson(message.getText());

        return Map.of("resume_json", result);
    }
}
