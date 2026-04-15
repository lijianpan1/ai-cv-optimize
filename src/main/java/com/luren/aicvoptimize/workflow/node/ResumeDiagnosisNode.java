package com.luren.aicvoptimize.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luren.aicvoptimize.agents.ResumeDiagnoseAgent;
import com.luren.aicvoptimize.dto.CvDiagnoseResponse;
import com.luren.aicvoptimize.util.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 简历诊断节点
 * <p>
 * 接收简历结构化数据和选中的目标职位信息，通过简历诊断智能体进行简历诊断，
 * 指出简历与目标职位之间的差异并提出优化建议。
 *
 * @author lijianpan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeDiagnosisNode implements NodeAction {

    private final ResumeDiagnoseAgent resumeDiagnoseAgent;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("=== 开始执行：简历诊断节点 ===");

        String resumeJson = (String) state.value("resume_json")
                .orElseThrow(() -> new IllegalArgumentException("缺少 resume_json"));
        String targetJob = (String) state.value("target_job")
                .orElseThrow(() -> new IllegalArgumentException("缺少 target_job，请先执行目标职位选择节点"));

        String resumeText = (String) state.value("resume_text").orElse(resumeJson);

        ReactAgent agent = resumeDiagnoseAgent.create(Map.of(
                "resume_text", resumeText,
                "target_job", targetJob));
        AssistantMessage message = agent.call("请根据简历和目标职位，诊断简历与目标职位的匹配度，并给出优化建议");
        String result = StrUtils.markdownToJson(message.getText());
        
        // 将 JSON 字符串解析为 CvDiagnoseResponse 对象，便于前端直接使用
        CvDiagnoseResponse diagnosisResult = objectMapper.readValue(result, CvDiagnoseResponse.class);
        log.info("简历诊断完成，综合评分: {}，问题数量: {}", 
                diagnosisResult.getOverallScore(), diagnosisResult.getIssues().size());

        return Map.of("diagnosis_result", diagnosisResult);
    }
}
