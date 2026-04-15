package com.luren.aicvoptimize.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * RAG检索器
 *
 * @author lijianpan
 **/
@RequiredArgsConstructor
@Component
public class RAGRetriever {

    private final VectorStore cusVectorStore;

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor() {
        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(cusVectorStore)
                        .build())
                .build();

        return retrievalAugmentationAdvisor;
    }
}