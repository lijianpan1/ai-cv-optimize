package com.luren.aicvoptimize.prompt;

/**
 * Prompt 资源索引。
 * <p>
 * 统一管理 key -> classpath 资源路径，避免散落在代码里写死字符串。
 */
public enum PromptKey {
    CV_SYSTEM("prompts/cv/system.txt"),
    CV_DIAGNOSE_USER("prompts/cv/diagnose_user.txt"),
    CV_OPTIMIZE_USER("prompts/cv/optimize_user.txt"),
    AGENT_CHAT_SYSTEM("prompts/agent/chat_system.txt"),
    RESUME_PARSER_AGENT("prompts/agent/resume_parser_agent.txt"),
    JOB_DETECTION_AGENT("prompts/agent/job_detection_agent.txt"),
    RESUME_OPTIMIZATION_AGENT("prompts/agent/resume_optimization_agent.txt"),
    RESUME_DIAGNOSIS_AGENT("prompts/agent/resume_diagnosis_agent.txt"),
    // 新增智能体Prompt
    INTERVIEWER_AGENT("prompts/agent/interviewer_agent.txt"),
    INTERVIEWER_EVALUATION_AGENT("prompts/agent/interviewer_evaluation_agent.txt"),
    CRITICAL_HR_AGENT("prompts/agent/critical_hr_agent.txt"),
    HIRING_MANAGER_AGENT("prompts/agent/hiring_manager_agent.txt"),
    ARBITRATOR_AGENT("prompts/agent/arbitrator_agent.txt");

    private final String classpathLocation;

    PromptKey(String classpathLocation) {
        this.classpathLocation = classpathLocation;
    }

    public String classpathLocation() {
        return classpathLocation;
    }
}

