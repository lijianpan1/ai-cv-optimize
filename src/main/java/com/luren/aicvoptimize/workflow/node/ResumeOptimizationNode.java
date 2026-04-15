package com.luren.aicvoptimize.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luren.aicvoptimize.agents.ResumeOptimizationAgent;
import com.luren.aicvoptimize.dto.JobRecommendationResponse;
import com.luren.aicvoptimize.util.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 简历优化节点
 * <p>
 * 接收简历结构化数据和职位信息，通过简历优化智能体生成优化后的简历，
 * 以结构化 JSON 输出到工作流状态，由下游 {@link ResumeMarkdownNode} 转换为 Markdown 格式。
 *
 * @author lijianpan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeOptimizationNode implements NodeAction {

    private final ResumeOptimizationAgent resumeOptimizationAgent;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("=== 开始执行：简历优化节点 ===");

        String resumeJson = (String) state.value("resume_json")
                .orElseThrow(() -> new IllegalArgumentException("缺少 resume_json"));

        // job_info 现在是 JobRecommendationResponse 对象，需要序列化为 JSON 字符串
        JobRecommendationResponse jobInfo = (JobRecommendationResponse) state.value("job_info")
                .orElseThrow(() -> new IllegalArgumentException("缺少 job_info，请先执行职位提取节点"));
        String jobInfoJson = objectMapper.writeValueAsString(jobInfo);

        // 从状态中获取可选的额外上下文（来自对抗节点的二次优化）
        String currentOptimizedResume = (String) state.value("current_optimized_resume").orElse("");
        String arbitrationSuggestions = (String) state.value("arbitration_suggestions").orElse("");

        // 构建仲裁建议上下文（统一处理所有可选输入）
        StringBuilder arbitrationContextBuilder = new StringBuilder();

        // 添加当前已优化的简历版本（如果存在）
        if (currentOptimizedResume != null && !currentOptimizedResume.isEmpty()) {
            arbitrationContextBuilder.append("=== 当前已优化的简历版本 ===\n").append(currentOptimizedResume).append("\n\n");
        }

        // 添加仲裁建议（如果存在）
        if (arbitrationSuggestions != null && !arbitrationSuggestions.isEmpty()) {
            arbitrationContextBuilder.append("=== 仲裁建议 ===\n").append(arbitrationSuggestions).append("\n\n");
        }

        String arbitrationContext = arbitrationContextBuilder.toString();

        // 构建变量映射
        Map<String, Object> variables = new HashMap<>();
        variables.put("resume_data", resumeJson);
        variables.put("job_description", jobInfoJson);
        variables.put("current_optimized_resume", (currentOptimizedResume == null || currentOptimizedResume.isEmpty()) ? "无" : currentOptimizedResume);
        variables.put("arbitration_suggestions", arbitrationContext.isEmpty() ? "无" : arbitrationContext);

        ReactAgent agent = resumeOptimizationAgent.create(variables);
        AssistantMessage message = agent.call("请按照指定的 JSON 结构输出优化后的简历数据");
        String result = StrUtils.markdownToJson(message.getText());

        log.info("简历优化完成，结构化 JSON 长度: {} 字符", result.length());

        return Map.of("optimized_resume_json", result);
    }
}
