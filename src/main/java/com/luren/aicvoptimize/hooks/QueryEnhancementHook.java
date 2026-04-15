package com.luren.aicvoptimize.hooks;

import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询增强钩子
 *
 * @author lijianpan
 **/
@RequiredArgsConstructor
@Component
@HookPositions({HookPosition.BEFORE_MODEL})
public class QueryEnhancementHook extends MessagesModelHook {
    private final ChatModel chatModel;
    private final VectorStore cusVectorStore;

    @Override
    public String getName() {
        return "message_trimming";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {

        // REPLACE 策略：替换所有现有消息
        return new AgentCommand(previousMessages, UpdatePolicy.REPLACE);
    }

    public Advisor customerRetrievalAugmentationAdvisor() {
        TranslationQueryTransformer translationQueryTransformer = TranslationQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .targetLanguage("Chinese")
                .build();

        CompressionQueryTransformer compressionQueryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();

        RewriteQueryTransformer rewriteQueryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();

        MultiQueryExpander multiQueryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();

        VectorStoreDocumentRetriever vectorStoreDocumentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(cusVectorStore)
                //查询语义相似的文档，基于相似性阈值、top-k、元数据进行过滤
                //过滤掉语义相似度阈值小于0.5的文档
                .similarityThreshold(0.50)
                .topK(3) // 返回的文档上限
//                        .filterExpression(new FilterExpressionBuilder()
//                                .eq("filename","李建潘")
//                                .build()) // 根据元数据进行过滤
                .build();

        ContextualQueryAugmenter contextualQueryAugmenter = ContextualQueryAugmenter.builder()
                // 允许空上下文
                .allowEmptyContext(true)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                // 查询翻译为目标语言
//                .queryTransformers(translationQueryTransformer)
                // 将上下文和查询压缩为一个独立的查询
//                .queryTransformers(compressionQueryTransformer)
                // 重写查询
//                .queryTransformers(rewriteQueryTransformer)
                // 多查询扩展
//                .queryExpander(multiQueryExpander)
                // 从向量数据库中检索与输入查询语义相似的文档
                .documentRetriever(vectorStoreDocumentRetriever)
                // 增强查询（将用户查询与提供的文档内容结合，生成一个增强后的查询）
                .queryAugmenter(contextualQueryAugmenter)
                .build();
    }
}