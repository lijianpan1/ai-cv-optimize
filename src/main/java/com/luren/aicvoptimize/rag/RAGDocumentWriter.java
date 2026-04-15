package com.luren.aicvoptimize.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档写入器
 *
 * @author lijianpan
 **/
@RequiredArgsConstructor
@Component
public class RAGDocumentWriter {

    private final VectorStore cusVectorStore;

    /**
     * 文档列表写入简单向量存储
     *
     */
    public String writeMemoryVectorStore(List<Document> documents) {
        // 将文档添加到向量存储中
        cusVectorStore.add(documents);
        String res = "成功将 "+documents.size()+" 个切片写入到向量数据库";
        return res;
    }

    /**
     * 文档列表写入到向量数据库
     **/
    public String writeDiskVectorStore(List<Document> documents) {
        // 将文档添加到向量存储中
        cusVectorStore.add(documents);
        String res = "成功将 "+documents.size()+" 个切片写入到向量数据库";
        return res;
    }

    /**
     * 相似性搜索
     * @param query 查询字符串
     * @return 返回与查询字符串相似的文档列表
     */
    public List<Document> search(String query) {
        // 构建相似性搜索请求并执行搜索
        return cusVectorStore.similaritySearch(SearchRequest
                .builder()
                .query(query)
                .similarityThreshold(0.6)
                .topK(3)
                .build());
    }
}