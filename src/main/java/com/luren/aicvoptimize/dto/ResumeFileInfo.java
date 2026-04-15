package com.luren.aicvoptimize.dto;

import lombok.Data;

/**
 * 简历文件基本信息
 *
 * @author lijianpan
 */
@Data
public class ResumeFileInfo {

    /** 文件原始名称（含扩展名） */
    private String originalFilename;

    /** 文件名（不含扩展名） */
    private String baseName;

    /** 文件扩展名（含点号，如 ".pdf"） */
    private String extension;

    /** 文件大小（字节） */
    private long size;

    /** 解析后的文本内容 */
    private String text;

    public ResumeFileInfo() {
    }

    public ResumeFileInfo(String originalFilename, String text, long size) {
        this.originalFilename = originalFilename;
        this.text = text;
        this.size = size;
        this.extension = resolveExtension(originalFilename);
        this.baseName = resolveBaseName(originalFilename);
    }

    private static String resolveExtension(String filename) {
        if (filename == null) return "";
        int dotIdx = filename.lastIndexOf('.');
        return dotIdx >= 0 ? filename.substring(dotIdx) : "";
    }

    private static String resolveBaseName(String filename) {
        if (filename == null) return "";
        int dotIdx = filename.lastIndexOf('.');
        return dotIdx > 0 ? filename.substring(0, dotIdx) : filename;
    }
}
