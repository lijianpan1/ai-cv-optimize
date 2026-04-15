package com.luren.aicvoptimize.dto;

import lombok.Data;

import java.util.List;

/**
 * 简历结构化数据。
 * <p>
 * 同时用于简历解析节点（输入原始文本 → 输出结构化数据）和简历优化节点（outputSchema 约束输出优化后的结构化数据）。
 */
@Data
public class ResumeParseResponse {
    /**
     * 基础信息
     */
    private BasicInfo basicInfo;

    /**
     * 教育经历列表
     */
    private List<Education> education;

    /**
     * 工作经历列表
     */
    private List<WorkExperience> workExperience;

    /**
     * 项目经历列表
     */
    private List<Project> projects;

    /**
     * 技能信息
     */
    private Skills skills;

    @Data
    public static class BasicInfo {
        /**
         * 姓名
         */
        private String name;

        /**
         * 电话
         */
        private String phone;

        /**
         * 邮箱
         */
        private String email;

        /**
         * 城市
         */
        private String city;

        /**
         * 求职目标
         */
        private String jobTarget;
    }

    @Data
    public static class Education {
        /**
         * 学校名称
         */
        private String school;

        /**
         * 专业
         */
        private String major;

        /**
         * 学历
         */
        private String degree;

        /**
         * 开始时间
         */
        private String startDate;

        /**
         * 结束时间
         */
        private String endDate;
    }

    @Data
    public static class WorkExperience {
        /**
         * 公司名称
         */
        private String company;

        /**
         * 职位
         */
        private String position;

        /**
         * 开始时间
         */
        private String startDate;

        /**
         * 结束时间
         */
        private String endDate;

        /**
         * 工作描述
         */
        private String description;

        /**
         * 经历亮点列表（优化后简历使用，每条为一条 STAR 描述）
         */
        private List<String> highlights;
    }

    @Data
    public static class Project {
        /**
         * 项目名称
         */
        private String name;

        /**
         * 项目角色
         */
        private String role;

        /**
         * 开始时间
         */
        private String startDate;

        /**
         * 结束时间
         */
        private String endDate;

        /**
         * 技术栈
         */
        private String techStack;

        /**
         * 项目描述
         */
        private String description;

        /**
         * 项目亮点列表（优化后简历使用，每条为一条 STAR 描述）
         */
        private List<String> highlights;
    }

    @Data
    public static class Skills {
        /**
         * 专业技能列表
         */
        private List<String> professionalSkills;

        /**
         * 语言技能列表
         */
        private List<String> languages;

        /**
         * 证书列表
         */
        private List<String> certificates;
    }
}
