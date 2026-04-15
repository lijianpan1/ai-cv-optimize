package com.luren.aicvoptimize.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luren.aicvoptimize.agents.InterviewerAgent;
import com.luren.aicvoptimize.dto.InterviewSession;
import com.luren.aicvoptimize.dto.JobRecommendationResponse;
import com.luren.aicvoptimize.util.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 面试预演节点
 * <p>
 * 基于优化后的简历和目标职位JD，生成高针对性的面试题库：
 * 1. 分析简历亮点和薄弱环节
 * 2. 根据职位要求生成技术问题、行为问题、情境问题
 * 3. 为每个问题设置评估标准和参考答案
 * <p>
 * 输入：optimized_resume_json、job_info
 * 输出：interview_session（面试会话数据）
 *
 * @author lijianpan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewRehearsalNode implements NodeAction {

    private final InterviewerAgent interviewerAgent;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("=== 开始执行：面试预演节点 ===");

        String optimizedResumeJson = (String) state.value("optimized_resume_json")
                .orElseThrow(() -> new IllegalArgumentException("缺少 optimized_resume_json"));

        JobRecommendationResponse jobInfo = (JobRecommendationResponse) state.value("job_info")
                .orElseThrow(() -> new IllegalArgumentException("缺少 job_info"));
        String jobDescription = objectMapper.writeValueAsString(jobInfo);

        // 生成面试题库
        InterviewSession session = generateInterviewQuestions(optimizedResumeJson, jobDescription);
        
        // 生成会话ID
        session.setSessionId(UUID.randomUUID().toString());
        
        String sessionJson = objectMapper.writeValueAsString(session);
        log.info("面试预演完成，生成 {} 道题目，会话ID: {}", 
                session.getQuestions() != null ? session.getQuestions().size() : 0,
                session.getSessionId());

        return Map.of("interview_session", sessionJson);
    }

    /**
     * 生成面试题库
     */
    private InterviewSession generateInterviewQuestions(
            String optimizedResume, String jobDescription) throws Exception {

        String schemaOutput = "{\n" +
                "  \"sessionId\": \"系统自动生成，留空\",\n" +
                "  \"targetJobTitle\": \"目标职位名称\",\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"questionId\": \"问题唯一ID，如q1, q2\",\n" +
                "      \"question\": \"问题内容\",\n" +
                "      \"type\": \"technical|behavioral|situational|project\",\n" +
                "      \"difficulty\": \"easy|medium|hard\",\n" +
                "      \"relatedSection\": \"workExperience|project|skill|education\",\n" +
                "      \"evaluationPoints\": [\"考察要点1\", \"考察要点2\"],\n" +
                "      \"referenceAnswer\": \"参考答案/评分要点\",\n" +
                "      \"weight\": 1-10的权重值\n" +
                "    }\n" +
                "  ],\n" +
                "  \"preparationAdvice\": \"面试准备建议，200字以内\",\n" +
                "  \"strengthsToHighlight\": [\"建议在面试中突出的优势1\", \"优势2\"],\n" +
                "  \"areasToImprove\": [\"需要准备的薄弱领域1\", \"领域2\"],\n" +
                "  \"estimatedDuration\": 预计面试时长（分钟）\n" +
                "}";
        
        Map<String, Object> variables = Map.of(
                "optimized_resume", optimizedResume,
                "job_description", jobDescription,
                "schemaOutput", schemaOutput
        );
        
        ReactAgent agent = interviewerAgent.createForQuestionGeneration(variables);
        AssistantMessage message = agent.call("请根据简历和职位描述生成针对性的面试题库");
        
        return objectMapper.readValue(StrUtils.markdownToJson(message.getText()), InterviewSession.class);
    }
}
