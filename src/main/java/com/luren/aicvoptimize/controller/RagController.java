package com.luren.aicvoptimize.controller;

import com.luren.aicvoptimize.rag.RAGDocumentReader;
import com.luren.aicvoptimize.rag.RAGDocumentTransformer;
import com.luren.aicvoptimize.rag.RAGDocumentWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Rag控制器
 *
 * @author lijianpan
 **/
@RequiredArgsConstructor
@RestController
@RequestMapping("/rag")
public class RagController {

    private final RAGDocumentReader documentReader;
    private final RAGDocumentTransformer documentTransformer;
    private final RAGDocumentWriter documentWriter;

    /**
     * 导入文本
     */
    @GetMapping("/rag/importText")
    public ResponseEntity<String> insertText(@RequestParam("text") String text) {
        // 1.文本验证
        if (!StringUtils.hasText(text)) {
            return ResponseEntity.badRequest().body("请输入文本");
        }
        // 2.解析文本
        List<Document> documents = List.of(new Document(text));

        // 3.切片文本
        List<Document> splitDocuments = documentTransformer
                .builder(documents)
                .transform()
                .get();

        // 4.写入向量存储
        documentWriter.writeMemoryVectorStore(splitDocuments);
        // 5.返回成功提示
        String msg = String.format("成功导入 %s 个文档", splitDocuments.size());
        return ResponseEntity.ok(msg);
    }

    /**
     * 导入文件
     * @param file
     * @return
     */
    @PostMapping(value = "/rag/importFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> insertFiles( @RequestPart(value = "file", required = false) MultipartFile file) {
        // 1. 文件验证
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("必须上传非空的文件");
        }
        // 2. 解析文件
        List<Document> documents = documentReader.readTika(file.getResource());

        // 3. 切片文本
        List<Document> splitDocuments = documentTransformer
                .builder(documents)
                .transform()
                .get();

        // 4. 写入向量存储
        documentWriter.writeMemoryVectorStore(splitDocuments);

        // 5.返回成功提示
        String msg = String.format("成功导入 %s 个文档", splitDocuments.size());
        return ResponseEntity.ok(msg);
    }

    /**
     * 导入文件V2
     * @param file
     * @return
     */
    @PostMapping(value = "/rag/importFileV2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importFileV2(@RequestPart(value = "file", required = false) MultipartFile file) {
        // 1. 文件验证
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("必须上传非空的文件");
        }
        // 2. 解析文件
        List<Document> documents = documentReader.readTika(file.getResource());

        // 3. 切片文本
        List<Document> splitDocuments = documentTransformer
                .builder(documents)
                .transform()
                .get();

        String fileId = UUID.randomUUID().toString();
        for (Document doc : splitDocuments) {
            doc.getMetadata().put("fileId", fileId);
        }

        // 4. 写入向量存储
        documentWriter.writeMemoryVectorStore(splitDocuments);

        // 5.返回成功提示
        String msg = String.format("成功导入 %s 个文档, 文件ID: %s", splitDocuments.size(), fileId);
        return ResponseEntity.ok(msg);
    }

}