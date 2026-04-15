package com.luren.aicvoptimize.config;

import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 输出Schema管理器
 * 统一管理所有BeanOutputConverter实例，支持动态扩展
 *
 * @author lijianpan
 */
@Component
public class OutputSchemaManager {

    private final Map<Class<?>, BeanOutputConverter<?>> converterCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, String> schemaCache = new ConcurrentHashMap<>();

    /**
     * 获取指定类型的BeanOutputConverter
     *
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return BeanOutputConverter实例
     */
    @SuppressWarnings("unchecked")
    public <T> BeanOutputConverter<T> getConverter(Class<T> clazz) {
        return (BeanOutputConverter<T>) converterCache.computeIfAbsent(clazz, 
            key -> new BeanOutputConverter<>(clazz));
    }

    /**
     * 获取指定类型的Schema字符串
     *
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return Schema字符串
     */
    public <T> String getSchema(Class<T> clazz) {
        return schemaCache.computeIfAbsent(clazz,
            key -> getConverter(clazz).getFormat());
    }

    /**
     * 预加载指定类型（可选，用于提前初始化）
     *
     * @param clazz 目标类型
     * @param <T>   泛型类型
     */
    public <T> void preload(Class<T> clazz) {
        getSchema(clazz);
    }

    /**
     * 清除缓存（主要用于测试）
     */
    public void clearCache() {
        converterCache.clear();
        schemaCache.clear();
    }
}
