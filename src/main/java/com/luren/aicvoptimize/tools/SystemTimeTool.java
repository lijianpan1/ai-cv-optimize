package com.luren.aicvoptimize.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 系统时间工具
 * <p>
 * 为智能体提供获取当前系统时间的能力
 */
@Slf4j
@Component
public class SystemTimeTool {

    private static final DateTimeFormatter DEFAULT_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy.MM");

    /**
     * 获取当前系统时间
     * <p>
     * 返回格式化的当前日期和时间，用于智能体判断时间逻辑
     *
     * @return 格式化后的当前时间字符串
     */
    @Tool(name = "get_current_time", 
          description = "获取当前系统时间（包含日期和时间）。用于判断简历中的日期是否合理，如项目时间是否在未来等。")
    public String getCurrentTime() {
        String result = LocalDateTime.now().format(DEFAULT_FORMATTER);
        log.info("[SystemTimeTool] 获取当前时间: {}", result);
        return result;
    }

    /**
     * 获取当前日期（便捷方法）
     */
    @Tool(name = "get_current_date",
          description = "获取当前系统日期（yyyy.MM）。用于判断简历中的项目时间、工作经历等日期是否合理。")
    public String getCurrentDate() {
        String result = LocalDateTime.now().format(DATE_ONLY_FORMATTER);
        log.info("[SystemTimeTool] 获取当前日期: {}", result);
        return result;
    }
}
