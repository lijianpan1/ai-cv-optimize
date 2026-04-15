package com.luren.aicvoptimize.prompt;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt 统一管理器：
 * - 从 classpath 读取 prompt 资源（UTF-8）
 * - 本地缓存，避免每次请求都读文件
 * - 支持简单占位符渲染：${key}
 */
@Component
public class PromptManager {

    private final ConcurrentHashMap<PromptKey, String> cache = new ConcurrentHashMap<>();

    /**
     * 获取模板内容
     * @param key 模板key
     * @return 模板内容
     */
    public String get(PromptKey key) {
        return cache.computeIfAbsent(key, this::loadUtf8);
    }

    /**
     * 渲染模板
     * @param key 模板key
     * @param variables 变量
     * @return 渲染后的模板
     */
    public String render(PromptKey key, Map<String, Object> variables) {
        String templateContent = get(key);
        PromptTemplate template = new PromptTemplate(templateContent);
        return template.create(variables).getContents();
    }

    /**
     * 从 classpath 加载 prompt 资源（UTF-8）
     * @param key 模板key
     * @return 模板内容
     */
    private String loadUtf8(PromptKey key) {
        ClassPathResource res = new ClassPathResource(key.classpathLocation());
        try {
            return StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load prompt: " + key + " from " + key.classpathLocation(), e);
        }
    }
}

