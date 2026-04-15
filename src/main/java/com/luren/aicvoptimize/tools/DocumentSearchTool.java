package com.luren.aicvoptimize.tools;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.luren.aicvoptimize.rag.RAGDocumentWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: RAG知识库搜索工具
 * @Description: TODO
 * @Author: lijianpan
 * @CreateTime: 2026-04-10  16:35
 * @Version: 1.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DocumentSearchTool {

    private final RAGDocumentWriter ragDocumentWriter;

    /**
     * 简历优化文档搜索请求
     */
    @JsonClassDescription("简历优化文档搜索请求参数")
    public record OptimizationDocumentRequest(
            @JsonProperty(required = true, value = "target_job_description")
            @JsonPropertyDescription("用户想要申请的具体职位描述（JD）文本或关键词。这是实现精准定制和关键词布局的基础，工具需要根据它来提取核心技能要求")
            String target_job_description,
            @JsonProperty(required = true, value = "current_profile")
            @JsonPropertyDescription("用户当前的职业身份或简历核心内容摘要。这有助于工具判断优化的方向（例如：是从技术转管理，还是应届生求职），从而提供符合该阶段的最佳实践")
            String current_profile,
            @JsonProperty(value = "resume_content")
            @JsonPropertyDescription("用户希望具体优化的简历段落或全文。如果用户没有提供具体内容，则搜索通用的优化框架和技巧")
            String resume_content
    ) {}

    /**
     * FAQ搜索请求
     */
    @JsonClassDescription("FAQ搜索请求参数")
    public record FAQsRequest(
            @JsonProperty(required = true, value = "career_level")
            @JsonPropertyDescription("用户当前的职业发展阶段。不同的阶段，高频问题截然不同")
            String career_level,
            @JsonProperty(value = "industry")
            @JsonPropertyDescription("用户求职的目标行业或领域。某些行业（如金融、设计、学术）有特定的简历禁忌和格式要求")
            String industry,
            @JsonProperty(value = "specific_concern")
            @JsonPropertyDescription("用户特别担心的具体问题或简历中的特定模块。如果用户未提供，则搜索该阶段的通用避坑指南")
            String specific_concern
    ) {}

    public String searchOptimizationDocument(OptimizationDocumentRequest request) {
        log.info("searchOptimizationDocument: target_job_description = {}, current_profile = {}, resume_content = {}",
                request.target_job_description(), request.current_profile(), request.resume_content());
        List<Document> documents = ragDocumentWriter.search(
                request.target_job_description() + " " + request.current_profile() + " " + request.resume_content());
        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
    }

    public String searchFAQs(FAQsRequest request) {
        log.info("searchFAQs: career_level = {}, industry = {}, specific_concern = {}",
                request.career_level(), request.industry(), request.specific_concern());
        List<Document> documents = ragDocumentWriter.search(
                request.career_level() + " " + request.industry() + " " + request.specific_concern());
        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
    }
}
