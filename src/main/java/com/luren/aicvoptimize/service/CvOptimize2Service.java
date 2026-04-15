package com.luren.aicvoptimize.service;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.luren.aicvoptimize.agents.JobDetectionAgent;
import com.luren.aicvoptimize.agents.ResumeDiagnoseAgent;
import com.luren.aicvoptimize.agents.ResumeOptimizationAgent;
import com.luren.aicvoptimize.agents.ResumeParserAgent;
import com.luren.aicvoptimize.dto.CvDiagnoseRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 简历优化服务
 *
 * @author lijianpan
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CvOptimize2Service {

    private final ResumeParserAgent resumeParserAgent;
    private final JobDetectionAgent jobDetectionAgent;
    private final ResumeDiagnoseAgent resumeDiagnoseAgent;
    private final ResumeOptimizationAgent resumeOptimizationAgent;

    /**
     * 解析文件
     *
     * @param file 简历文件
     * @return 简历内容
     */
    public String optimize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(file.getResource());
        List<Document> documents = pdfReader.read();

        return documents.stream()
                .map(doc -> "=== 第 " + doc.getMetadata().get("page_number") + " 页 ===\n" + doc.getText())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 简历结构化输出
     */
    public String resumeStructure(String content) throws GraphRunnerException {
        ReactAgent agent = resumeParserAgent.create(Map.of("resume_text", content));
        AssistantMessage message = agent.call("请生成一个结构化JSON数据");
        return message.getText();
    }

    /**
     * 简历结构化输出
     */
    public String resumeStructure(MultipartFile file) throws GraphRunnerException {
        String content = optimize(file);
        return resumeStructure(content);
    }

    /**
     * 识别职位
     */
    public String detectJob(String content) throws GraphRunnerException {
        ReactAgent agent = jobDetectionAgent.create(Map.of("resume_json", content));
        AssistantMessage message = agent.call("请返回匹配的职位信息");
        return message.getText();
    }

    /**
     * 简历诊断
     */
    public String resumeDiagnose(CvDiagnoseRequest request) throws GraphRunnerException {
        ReactAgent agent = resumeDiagnoseAgent.create(Map.of(
                "resume_text", request.getResumeText(),
                "target_job", request.getTargetJobTitle()
        ));
        AssistantMessage message = agent.call("请根据简历和目标职位，诊断简历与目标职位的匹配度，并给出优化建议");
        return message.getText();
    }

    /**
     * 简历优化
     */
    public String resumeOptimize(String content, String job) throws GraphRunnerException {
        ReactAgent agent = resumeOptimizationAgent.create(Map.of(
                "resume_data", content,
                "job_description", job
        ));
        AssistantMessage message = agent.call("请按照指定的 JSON 结构输出优化后的简历数据");
        return message.getText();
    }
}
