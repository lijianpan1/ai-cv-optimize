package com.luren.aicvoptimize.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luren.aicvoptimize.agents.ArbitratorAgent;
import com.luren.aicvoptimize.agents.CriticalHrAgent;
import com.luren.aicvoptimize.agents.HiringManagerAgent;
import com.luren.aicvoptimize.agents.ResumeOptimizationAgent;
import com.luren.aicvoptimize.dto.AdversarialEvaluationResult;
import com.luren.aicvoptimize.dto.JobRecommendationResponse;
import com.luren.aicvoptimize.util.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对抗性优化节点
 * <p>
 * 实现红蓝军对抗机制：
 * 1. 生成三个版本的优化方案（保守、平衡、进取）
 * 2. HR智能体（红军）攻击简历弱点
 * 3. 业务主管智能体（蓝军）辩护候选人潜力
 * 4. 仲裁智能体综合评判，选择最佳版本
 * <p>
 * 输入：resume_json（原始简历）、job_info（职位信息）、optimized_resume_json（初始优化版本）
 * 输出：adversarial_optimization_result（对抗性优化结果）、optimized_resume_json（根据仲裁建议重新优化后的简历）
 *
 * @author lijianpan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdversarialOptimizationNode implements NodeAction {

    private final CriticalHrAgent criticalHrAgent;
    private final HiringManagerAgent hiringManagerAgent;
    private final ArbitratorAgent arbitratorAgent;
    private final ResumeOptimizationAgent resumeOptimizationAgent;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("=== 开始执行：对抗性优化节点 ===");

        String resumeJson = (String) state.value("resume_json")
                .orElseThrow(() -> new IllegalArgumentException("缺少 resume_json"));
        
        String optimizedResumeJson = (String) state.value("optimized_resume_json")
                .orElseThrow(() -> new IllegalArgumentException("缺少 optimized_resume_json"));

        JobRecommendationResponse jobInfo = (JobRecommendationResponse) state.value("job_info")
                .orElseThrow(() -> new IllegalArgumentException("缺少 job_info"));
        String jobDescription = objectMapper.writeValueAsString(jobInfo);

        // 步骤1: HR视角评估（红军攻击）
        log.info("步骤1: HR视角评估（红军攻击）");
        AdversarialEvaluationResult.PerspectiveEvaluation hrEvaluation = 
                evaluateFromHrPerspective(optimizedResumeJson, jobDescription);

        // 步骤2: 业务主管视角评估（蓝军辩护）
        log.info("步骤2: 业务主管视角评估（蓝军辩护）");
        AdversarialEvaluationResult.PerspectiveEvaluation managerEvaluation = 
                evaluateFromManagerPerspective(optimizedResumeJson, jobDescription);

        // 步骤3: 仲裁智能体综合评判
        log.info("步骤3: 仲裁智能体综合评判");
        AdversarialEvaluationResult.ArbitrationResult arbitrationResult = 
                arbitrate(optimizedResumeJson, jobDescription, hrEvaluation, managerEvaluation);

        // 构建最终结果
        AdversarialEvaluationResult result = new AdversarialEvaluationResult();
        result.setVersionId("v1");
        result.setOverallScore(arbitrationResult.getFinalScore());
        result.setHrPerspective(hrEvaluation);
        result.setManagerPerspective(managerEvaluation);
        result.setArbitrationResult(arbitrationResult);
        result.setFinalRecommendations(arbitrationResult.getRequiredChanges());
        result.setRiskWarnings(buildRiskWarnings(hrEvaluation, arbitrationResult));
        result.setApproved(arbitrationResult.getFinalScore() >= 70);

        log.info("对抗性优化完成，最终评分: {}, 是否通过: {}", 
                result.getOverallScore(), result.isApproved());

        // 步骤4: 根据仲裁建议重新优化简历
        log.info("步骤4: 根据仲裁建议重新优化简历");
        String reOptimizedResume = reOptimizeResume(
                resumeJson, optimizedResumeJson, jobDescription, arbitrationResult);
        log.info("根据仲裁建议重新优化简历完成");

        // 直接返回对象，让 Jackson 自动序列化为 JSON
        return Map.of(
                "adversarial_optimization_result", result,
                "optimized_resume_json", reOptimizedResume
        );
    }

    /**
     * HR视角评估（红军攻击）
     */
    private AdversarialEvaluationResult.PerspectiveEvaluation evaluateFromHrPerspective(
            String resumeVersion, String jobDescription) throws Exception {

        String schemaOutput = "{\n" +
                "  \"score\": 评分(0-100，从HR风险视角打分，分数越低风险越高),\n" +
                "  \"pros\": [\"认可的优点1\", \"优点2\"],\n" +
                "  \"cons\": [\"发现的问题1\", \"问题2\"],\n" +
                "  \"commentary\": \"整体评价（150字以内）\",\n" +
                "  \"keyPoints\": [\"攻击要点1：具体描述\", \"攻击要点2：具体描述\"]\n" +
                "}";

        Map<String, Object> variables = Map.of(
                "resume_version", resumeVersion,
                "job_description", jobDescription,
                "schemaOutput", schemaOutput
        );

        ReactAgent agent = criticalHrAgent.create(variables);

        AssistantMessage message = agent.call("请从HR视角评估这份简历，找出潜在问题和风险点");
        return objectMapper.readValue(StrUtils.markdownToJson(message.getText()),
                AdversarialEvaluationResult.PerspectiveEvaluation.class);
    }

    /**
     * 业务主管视角评估（蓝军辩护）
     */
    private AdversarialEvaluationResult.PerspectiveEvaluation evaluateFromManagerPerspective(
            String resumeVersion, String jobDescription) throws Exception {

        String schemaOutput = "{\n" +
                "  \"score\": 评分(0-100，从业务价值视角打分，分数越高价值越大),\n" +
                "  \"pros\": [\"候选人优势1\", \"优势2\"],\n" +
                "  \"cons\": [\"可以理解的不足1\", \"不足2\"],\n" +
                "  \"commentary\": \"整体评价（150字以内）\",\n" +
                "  \"keyPoints\": [\"辩护要点1：具体描述\", \"辩护要点2：具体描述\"]\n" +
                "}";

        Map<String, Object> variables = Map.of(
                "resume_version", resumeVersion,
                "job_description", jobDescription,
                "schemaOutput", schemaOutput
        );

        ReactAgent agent = hiringManagerAgent.create(variables);

        AssistantMessage message = agent.call("请从业务主管视角评估这份简历，突出候选人的潜力和价值");
        return objectMapper.readValue(StrUtils.markdownToJson(message.getText()),
                AdversarialEvaluationResult.PerspectiveEvaluation.class);

    }

    /**
     * 仲裁智能体综合评判
     */
    private AdversarialEvaluationResult.ArbitrationResult arbitrate(
            String resumeVersion, String jobDescription,
            AdversarialEvaluationResult.PerspectiveEvaluation hrEvaluation,
            AdversarialEvaluationResult.PerspectiveEvaluation managerEvaluation) throws Exception {

        String schemaOutput = "{\n" +
                "  \"finalScore\": 最终评分(0-100),\n" +
                "  \"reasoning\": \"仲裁理由（200字以内，说明如何权衡两种观点）\",\n" +
                "  \"requiredChanges\": [\"必须修改项1\", \"修改项2\"],\n" +
                "  \"highlightsToKeep\": [\"应保留的亮点1\", \"亮点2\"],\n" +
                "  \"riskLevel\": \"low|medium|high\"\n" +
                "}";
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("resume_version", resumeVersion);
        variables.put("job_description", jobDescription);
        variables.put("hr_evaluation", objectMapper.writeValueAsString(hrEvaluation));
        variables.put("manager_evaluation", objectMapper.writeValueAsString(managerEvaluation));
        variables.put("schemaOutput", schemaOutput);
        
        ReactAgent agent = arbitratorAgent.create(variables);

        AssistantMessage message = agent.call("请综合HR视角和业务主管视角的评估结果，进行最终仲裁");

        return objectMapper.readValue(StrUtils.markdownToJson(message.getText()),
                AdversarialEvaluationResult.ArbitrationResult.class);
    }

    /**
     * 构建风险警告列表
     */
    private List<String> buildRiskWarnings(
            AdversarialEvaluationResult.PerspectiveEvaluation hrEvaluation,
            AdversarialEvaluationResult.ArbitrationResult arbitrationResult) {
        
        List<String> warnings = new ArrayList<>();
        
        // 添加HR视角识别的风险
        if (hrEvaluation.getCons() != null) {
            warnings.addAll(hrEvaluation.getCons().stream()
                    .filter(con -> con.contains("风险") || con.contains("疑虑") || con.contains("问题"))
                    .limit(3)
                    .toList());
        }
        
        // 添加仲裁结果中的风险等级提示
        if ("high".equals(arbitrationResult.getRiskLevel())) {
            warnings.add("该简历存在较高风险，建议进一步优化");
        } else if ("medium".equals(arbitrationResult.getRiskLevel())) {
            warnings.add("该简历存在一定风险，请关注仲裁建议");
        }
        
        return warnings;
    }

    /**
     * 根据仲裁建议重新优化简历
     *
     * @param originalResumeJson    原始简历JSON
     * @param currentOptimizedJson  当前优化版本JSON
     * @param jobDescription        职位描述
     * @param arbitrationResult     仲裁结果，包含必须修改项和应保留亮点
     * @return 重新优化后的简历JSON
     */
    private String reOptimizeResume(
            String originalResumeJson,
            String currentOptimizedJson,
            String jobDescription,
            AdversarialEvaluationResult.ArbitrationResult arbitrationResult) throws Exception {
        
        if (arbitrationResult == null) {
            throw new IllegalArgumentException("仲裁结果不能为空");
        }

        // 构建仲裁建议上下文
        StringBuilder arbitrationContext = new StringBuilder();
        arbitrationContext.append("=== 仲裁优化建议 ===\n\n");

        // 必须修改的要点
        if (arbitrationResult.getRequiredChanges() != null && !arbitrationResult.getRequiredChanges().isEmpty()) {
            arbitrationContext.append("【必须修改的要点】\n");
            for (int i = 0; i < arbitrationResult.getRequiredChanges().size(); i++) {
                arbitrationContext.append(i + 1).append(". ")
                        .append(arbitrationResult.getRequiredChanges().get(i)).append("\n");
            }
            arbitrationContext.append("\n");
        }

        // 建议保留的亮点
        if (arbitrationResult.getHighlightsToKeep() != null && !arbitrationResult.getHighlightsToKeep().isEmpty()) {
            arbitrationContext.append("【建议保留的亮点】\n");
            for (int i = 0; i < arbitrationResult.getHighlightsToKeep().size(); i++) {
                arbitrationContext.append(i + 1).append(". ")
                        .append(arbitrationResult.getHighlightsToKeep().get(i)).append("\n");
            }
            arbitrationContext.append("\n");
        }

        // 仲裁理由
        if (arbitrationResult.getReasoning() != null) {
            arbitrationContext.append("【仲裁理由】\n")
                    .append(arbitrationResult.getReasoning()).append("\n\n");
        }

        // 风险等级
        arbitrationContext.append("【风险等级】\n")
                .append(arbitrationResult.getRiskLevel()).append("\n");

        // 调用简历优化智能体，传入仲裁建议
        Map<String, Object> variables = new HashMap<>();
        variables.put("resume_data", originalResumeJson);
        variables.put("job_description", jobDescription);
        variables.put("current_optimized_resume", currentOptimizedJson);
        variables.put("arbitration_suggestions", arbitrationContext.toString());

        ReactAgent agent = resumeOptimizationAgent.create(variables);
        AssistantMessage message = agent.call("请输出优化后的结构化简历数据");
        return message.getText();
    }
}
