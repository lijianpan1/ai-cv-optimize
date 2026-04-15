package com.luren.aicvoptimize.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.luren.aicvoptimize.config.OutputSchemaManager;
import com.luren.aicvoptimize.dto.ResumeParseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 简历 Markdown 转换节点
 * <p>
 * 读取工作流状态中的结构化简历 JSON（optimized_resume_json），
 * 将其转换为标准 Markdown 格式，供下游文件生成节点使用。
 *
 * @author lijianpan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeMarkdownNode implements NodeAction {

    private final OutputSchemaManager schemaManager;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("=== 开始执行：简历 Markdown 转换节点 ===");

        String resumeJson = (String) state.value("optimized_resume_json")
                .orElseThrow(() -> new IllegalArgumentException("缺少 optimized_resume_json，请先执行简历优化节点"));

        ResumeParseResponse resume = schemaManager.getConverter(ResumeParseResponse.class)
                .convert(resumeJson);

        String markdown = toMarkdown(resume);
        log.info("Markdown 转换完成，内容长度: {} 字符", markdown.length());

        return Map.of("optimized_resume", markdown);
    }

    /**
     * 将结构化简历数据转换为 Markdown 格式。
     * <p>
     * Markdown 层级约定（与 ResumeFileGenerator 解析逻辑对齐）：
     * <ul>
     *   <li>{@code #} — 姓名（一级标题，居中）</li>
     *   <li>{@code ##} — 章节标题（带分隔线）</li>
     *   <li>{@code ###} — 子标题（公司/项目名）</li>
     *   <li>{@code - } — 列表项（经历亮点、技能标签）</li>
     * </ul>
     */
    private String toMarkdown(ResumeParseResponse resume) {
        StringBuilder md = new StringBuilder();

        // # 姓名
        ResumeParseResponse.BasicInfo info = resume.getBasicInfo();
        if (info != null && info.getName() != null) {
            md.append("# ").append(info.getName()).append("\n\n");
            // 联系方式 + 求职目标 单行展示
            List<String> contacts = new ArrayList<>();
            if (info.getPhone() != null) contacts.add(info.getPhone());
            if (info.getEmail() != null) contacts.add(info.getEmail());
            if (info.getCity() != null) contacts.add(info.getCity());
            if (!contacts.isEmpty()) {
                md.append(contacts.stream().collect(Collectors.joining(" | "))).append("\n\n");
            }
            // 求职目标
            if (info.getJobTarget() != null && !info.getJobTarget().isBlank()) {
                md.append("## 求职意向\n\n");
                md.append("- ").append(info.getJobTarget()).append("\n\n");
            }
        }

        // ## 专业技能
        ResumeParseResponse.Skills skills = resume.getSkills();
        if (skills != null) {
            md.append("## 专业技能\n\n");
            appendSkillList(md, "专业技能", skills.getProfessionalSkills());
//            appendSkillList(md, "语言能力", skills.getLanguages());
//            appendSkillList(md, "证书资质", skills.getCertificates());
            md.append("\n");
        }

        // ## 工作经历
        List<ResumeParseResponse.WorkExperience> workList = resume.getWorkExperience();
        if (workList != null && !workList.isEmpty()) {
            md.append("## 工作经历\n\n");
            for (ResumeParseResponse.WorkExperience work : workList) {
                md.append("### ").append(work.getCompany()).append("\n\n");
                md.append("**").append(work.getPosition()).append("**");
                appendDateRange(md, work.getStartDate(), work.getEndDate());
                md.append("\n\n");
                appendHighlights(md, work.getHighlights(), work.getDescription());
                md.append("\n");
            }
        }

        // ## 项目经历
        List<ResumeParseResponse.Project> projList = resume.getProjects();
        if (projList != null && !projList.isEmpty()) {
            md.append("## 项目经历\n\n");
            for (ResumeParseResponse.Project proj : projList) {
                md.append("### ").append(proj.getName()).append("\n\n");
                md.append("**").append(proj.getRole()).append("**");
                appendDateRange(md, proj.getStartDate(), proj.getEndDate());
                md.append("\n");
                if (proj.getTechStack() != null && !proj.getTechStack().isBlank()) {
                    md.append("技术栈：").append(proj.getTechStack()).append("\n\n");
                }
                appendHighlights(md, proj.getHighlights(), proj.getDescription());
                md.append("\n");
            }
        }

        // ## 教育背景
        List<ResumeParseResponse.Education> eduList = resume.getEducation();
        if (eduList != null && !eduList.isEmpty()) {
            md.append("## 教育背景\n\n");
            for (ResumeParseResponse.Education edu : eduList) {
                md.append("### ").append(edu.getSchool()).append("\n\n");
                md.append("**").append(edu.getMajor()).append("** | ").append(edu.getDegree());
                appendDateRange(md, edu.getStartDate(), edu.getEndDate());
                md.append("\n\n");
            }
        }

        return md.toString().stripTrailing();
    }

    private void appendSkillList(StringBuilder md, String label, List<String> items) {
        if (items != null && !items.isEmpty()) {
            md.append("- ")
//                    .append(label)
//                    .append("：")
                    .append(String.join("\n- ", items))
                    .append("\n");
        }
    }

    private void appendDateRange(StringBuilder md, String startDate, String endDate) {
        md.append(" | ");
        md.append(startDate != null ? startDate : "");
        md.append(" - ");
        md.append(endDate != null ? endDate : "");
    }

    /**
     * 优先使用 highlights 列表（优化后的多条 STAR 描述），
     * 若为空则回退到 description 单行文本。
     */
    private void appendHighlights(StringBuilder md, List<String> highlights, String description) {
        if (highlights != null && !highlights.isEmpty()) {
            for (String h : highlights) {
                if (h != null && !h.isBlank()) {
                    md.append("- ").append(h).append("\n");
                }
            }
        } else if (description != null && !description.isBlank()) {
            md.append("- ").append(description).append("\n");
        }
    }
}
