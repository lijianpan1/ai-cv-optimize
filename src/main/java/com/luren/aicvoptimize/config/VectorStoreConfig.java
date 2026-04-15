package com.luren.aicvoptimize.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 简单向量存储配置
 *
 * @author lijianpan
 **/
@RequiredArgsConstructor
@Configuration
public class VectorStoreConfig {
    private final EmbeddingModel DashScopeEmbeddingModel;
    private final VectorStore vectorStore;

    /**
     * 创建简单向量存储
     *
     * @return 简单向量存储实例
     */
//    @Bean
//    public VectorStore cusVectorStore() {
//        return SimpleVectorStore.builder(DashScopeEmbeddingModel).build();
//    }

    /**
     * 创建PGSql向量存储
     *
     * @return PGSql向量存储实例
     */
    @Bean
    public VectorStore cusVectorStore() {
        return vectorStore;
    }
}