package com.luren.aicvoptimize.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.DefaultContentFormatter;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.transformer.ContentFormatTransformer;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 文档转换器 - 支持链式调用进行文档处理
 *
 * @author lijianpan
 **/
@RequiredArgsConstructor
@Component
public class RAGDocumentTransformer {
    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_MIN_CHUNK_SIZE_CHARS = 350;
    private static final int DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED = 5;
    private static final int DEFAULT_MAX_NUM_CHUNKS = 10000;
    private static final int DEFAULT_KEYWORD_COUNT = 3;

    private final ChatModel chatModel;

    /**
     * 创建链式调用构建器
     *
     * @param documents 待处理的文档列表
     * @return 构建器实例
     * @author lijianpan
     **/
    public Builder builder(List<Document> documents) {
        return new Builder(documents, this);
    }

    /**
     * 文档切片
     *
     * @author lijianpan
     **/
    public List<Document> tokenTextSplitter(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                .withChunkSize(DEFAULT_CHUNK_SIZE)
                .withMinChunkSizeChars(DEFAULT_MIN_CHUNK_SIZE_CHARS)
                .withMinChunkLengthToEmbed(DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED)
                .withMaxNumChunks(DEFAULT_MAX_NUM_CHUNKS)
                .withKeepSeparator(true)
                .build();

        return tokenTextSplitter.split(documents);
    }

    /**
     * 文档内容格式转换
     *
     * @author lijianpan
     **/
    public List<Document> contentFormatTransformer(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        DefaultContentFormatter defaultContentFormatter = DefaultContentFormatter.defaultConfig();
        ContentFormatTransformer contentFormatTransformer = new ContentFormatTransformer(defaultContentFormatter);
        return contentFormatTransformer.apply(documents);
    }

    /**
     * 文档内容关键词提取
     *
     * @author lijianpan
     **/
    public List<Document> keywordMetadataEnricher(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(chatModel, DEFAULT_KEYWORD_COUNT);
        return keywordMetadataEnricher.apply(documents);
    }

    /**
     * 文档内容摘要提取
     *
     * @author lijianpan
     **/
    public List<Document> summaryMetadataEnricher(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<SummaryMetadataEnricher.SummaryType> summaryTypes = List.of(
                SummaryMetadataEnricher.SummaryType.NEXT,
                SummaryMetadataEnricher.SummaryType.CURRENT,
                SummaryMetadataEnricher.SummaryType.PREVIOUS);

        SummaryMetadataEnricher summaryMetadataEnricher = new SummaryMetadataEnricher(chatModel, summaryTypes);
        return summaryMetadataEnricher.apply(documents);
    }

    /**
     * RAG 常用的默认转换链：切片 -> 内容格式化 -> 关键词 -> 摘要
     *
     * @author lijianpan
     **/
    public List<Document> transformForRag(List<Document> documents) {
        Objects.requireNonNull(documents, "documents must not be null");

        List<Document> transformed = tokenTextSplitter(documents);
        transformed = contentFormatTransformer(transformed);
        transformed = keywordMetadataEnricher(transformed);
        transformed = summaryMetadataEnricher(transformed);
        return transformed;
    }

    /**
     * 链式调用构建器
     *
     * @author lijianpan
     **/
    public static class Builder {
        private List<Document> documents;
        private final RAGDocumentTransformer transformer;
        private int chunkSize = DEFAULT_CHUNK_SIZE;
        private int minChunkSizeChars = DEFAULT_MIN_CHUNK_SIZE_CHARS;
        private int minChunkLengthToEmbed = DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED;
        private int maxNumChunks = DEFAULT_MAX_NUM_CHUNKS;
        private boolean keepSeparator = true;

        private Builder(List<Document> documents, RAGDocumentTransformer transformer) {
            this.documents = documents;
            this.transformer = transformer;
        }

        /**
         * 设置分块大小
         *
         * @param chunkSize 每个文本块的目标token数量
         * @return 构建器实例
         * @author lijianpan
         **/
        public Builder chunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        /**
         * 设置最小分块字符数
         *
         * @param minChunkSizeChars 每个文本块的最小字符数
         * @return 构建器实例
         * @author lijianpan
         **/
        public Builder minChunkSizeChars(int minChunkSizeChars) {
            this.minChunkSizeChars = minChunkSizeChars;
            return this;
        }

        /**
         * 设置最小嵌入长度
         *
         * @param minChunkLengthToEmbed 丢弃小于此长度的文本块
         * @return 构建器实例
         * @author lijianpan
         **/
        public Builder minChunkLengthToEmbed(int minChunkLengthToEmbed) {
            this.minChunkLengthToEmbed = minChunkLengthToEmbed;
            return this;
        }

        /**
         * 设置最大分块数
         *
         * @param maxNumChunks 文本中生成的最大块数
         * @return 构建器实例
         * @author lijianpan
         **/
        public Builder maxNumChunks(int maxNumChunks) {
            this.maxNumChunks = maxNumChunks;
            return this;
        }

        /**
         * 是否保留分隔符
         *
         * @param keepSeparator 是否保留分隔符
         * @return 构建器实例
         * @author lijianpan
         **/
        public Builder keepSeparator(boolean keepSeparator) {
            this.keepSeparator = keepSeparator;
            return this;
        }

        /**
         * 执行文档切片
         *
         * @return 构建器实例
         * @author lijianpan
         **/
        public Builder split() {
            TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                    .withChunkSize(chunkSize)
                    .withMinChunkSizeChars(minChunkSizeChars)
                    .withMinChunkLengthToEmbed(minChunkLengthToEmbed)
                    .withMaxNumChunks(maxNumChunks)
                    .withKeepSeparator(keepSeparator)
                    .build();

            documents = tokenTextSplitter.split(this.documents);
            return this;
        }

        /**
         * 执行内容格式转换
         *
         * @return 构建器实例
         * @author lijianpan
         **/
        public Builder format() {
            DefaultContentFormatter defaultContentFormatter = DefaultContentFormatter.defaultConfig();
            ContentFormatTransformer contentFormatTransformer = new ContentFormatTransformer(defaultContentFormatter);
            documents = contentFormatTransformer.apply(this.documents);
            return this;
        }

        /**
         * 执行关键词提取
         *
         * @return 构建器实例
         * @author lijianpan
         **/
        public Builder extractKeywords() {
            return extractKeywords(DEFAULT_KEYWORD_COUNT);
        }

        /**
         * 执行关键词提取
         *
         * @param keywordCount 提取的关键词数量
         * @return 构建器实例
         * @author lijianpan
         **/
        public Builder extractKeywords(int keywordCount) {
            KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(transformer.chatModel, keywordCount);
            documents = keywordMetadataEnricher.apply(this.documents);
            return this;
        }

        /**
         * 执行摘要生成
         *
         * @return 构建器实例
         * @author lijianpan
         **/
        public Builder generateSummary() {
            List<SummaryMetadataEnricher.SummaryType> summaryTypes = List.of(
                    SummaryMetadataEnricher.SummaryType.NEXT,
                    SummaryMetadataEnricher.SummaryType.CURRENT,
                    SummaryMetadataEnricher.SummaryType.PREVIOUS);

            return generateSummary(summaryTypes);
        }

        /**
         * 执行摘要生成
         *
         * @param summaryTypes 摘要类型列表
         * @return 构建器实例
         * @author lijianpan
         **/
        public Builder generateSummary(List<SummaryMetadataEnricher.SummaryType> summaryTypes) {
            SummaryMetadataEnricher summaryMetadataEnricher = new SummaryMetadataEnricher(transformer.chatModel, summaryTypes);
            documents = summaryMetadataEnricher.apply(this.documents);
            return this;
        }

        /**
         * 执行完整的 RAG 转换流程
         *
         * @return 构建器实例
         * @author lijianpan
         **/
        public Builder transform() {
            return split().format().extractKeywords().generateSummary();
        }

        /**
         * 获取处理结果
         *
         * @return 处理后的文档列表
         * @author lijianpan
         **/
        public List<Document> get() {
            return this.documents;
        }
    }
}
