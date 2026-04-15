package com.luren.aicvoptimize.workflow.node.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PDF 文档解析器
 * <p>
 * 使用 {@link PagePdfDocumentReader} 按页解析 PDF，
 * 通过页码标注保留文档结构，提升后续 AI 解析的准确性。
 *
 * @author lijianpan
 */
@Slf4j
@Component
public class PdfDocumentParser implements DocumentParser {

    @Override
    public String[] supportedExtensions() {
        return new String[]{"pdf"};
    }

    @Override
    public String parse(MultipartFile file) {
        log.info("开始解析 PDF 文件: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

        PagePdfDocumentReader reader = new PagePdfDocumentReader(file.getResource());
        List<Document> documents = reader.read();

        String text = documents.stream()
                .map(doc -> {
                    Object pageNumber = doc.getMetadata().get("page_number");
                    String pageLabel = pageNumber != null ? "第 " + pageNumber + " 页" : "未知页";
                    return "=== " + pageLabel + " ===\n" + doc.getText();
                })
                .collect(Collectors.joining("\n\n"));

        log.info("PDF 解析完成，共 {} 页，提取文本长度: {}", documents.size(), text.length());
        return text;
    }
}
