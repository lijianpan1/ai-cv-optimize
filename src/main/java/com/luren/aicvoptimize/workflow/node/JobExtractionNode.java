package com.luren.aicvoptimize.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luren.aicvoptimize.agents.JobDetectionAgent;
import com.luren.aicvoptimize.dto.JobRecommendationResponse;
import com.luren.aicvoptimize.util.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 职位提取节点
 * <p>
 * 接收简历解析后的结构化数据，通过职位识别智能体提取匹配的目标职位信息
 *
 * @author lijianpan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobExtractionNode implements NodeAction {

    private final JobDetectionAgent jobDetectionAgent;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("=== 开始执行：职位提取节点 ===");
        String resumeJson = (String) state.value("resume_json")
                .orElseThrow(() -> new IllegalArgumentException("缺少 resume_json，请先执行简历解析节点"));

        ReactAgent agent = jobDetectionAgent.create(Map.of("resume_json", resumeJson));
        AssistantMessage message = agent.call("请返回匹配的职位信息");
        String result = StrUtils.markdownToJson(message.getText());

        // 将 JSON 字符串解析为 JobRecommendationResponse 对象，便于前端直接使用
        JobRecommendationResponse jobInfo = objectMapper.readValue(result, JobRecommendationResponse.class);
        log.info("职位提取完成，推荐职位数量: {}", jobInfo.getRecommendedJobs().size());

        return Map.of("job_info", jobInfo);
    }
}
