package com.luren.aicvoptimize.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 工作流状态 DTO
 * <p>
 * 用于前后端传递工作流执行状态
 */
@Data
public class WorkflowState {

    private String threadId;
    
    @JsonProperty("resume_text")
    private String resumeText;
    
    @JsonProperty("resume_json")
    private Object resumeJson;
    
    @JsonProperty("job_info")
    private Object jobInfo;
    
    @JsonProperty("target_job")
    private Object targetJob;
    
    @JsonProperty("diagnosis_result")
    private Object diagnosisResult;
    
    @JsonProperty("optimized_resume_json")
    private Object optimizedResumeJson;
    
    @JsonProperty("adversarial_optimization_result")
    private Object adversarialOptimizationResult;
    
    @JsonProperty("optimized_resume")
    private String optimizedResume;
    
    @JsonProperty("interview_session")
    private Object interviewSession;
    
    @JsonProperty("pdf_path")
    private String pdfPath;
    
    @JsonProperty("word_path")
    private String wordPath;
    
    @JsonProperty("pdf_download_url")
    private String pdfDownloadUrl;
    
    @JsonProperty("word_download_url")
    private String wordDownloadUrl;
}
