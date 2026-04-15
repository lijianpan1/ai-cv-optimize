package com.luren.aicvoptimize.rag;


import com.luren.aicvoptimize.constant.RagDocumentConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档读取器
 *
 * @author lijianpan
 **/
@Component
@Slf4j
public class RAGDocumentReader {

    public List<Document> readText() {
        log.info("start read text file");
        Resource resource = new DefaultResourceLoader().getResource(RagDocumentConstant.TEXT_FILE_PATH);
        TextReader textReader = new TextReader(resource); // 适用于文本数据
        return textReader.read();
    }

    public List<Document> readJson() {
        log.info("start read json file");
        Resource resource = new DefaultResourceLoader().getResource(RagDocumentConstant.JSON_FILE_PATH);
        JsonReader jsonReader = new JsonReader(resource); // 只可以传json格式文件
        return jsonReader.read();
    }

    public List<Document> readPdfPage() {
        log.info("start read pdf file by page");
        Resource resource = new DefaultResourceLoader().getResource(RagDocumentConstant.PDF_FILE_PATH);
        PagePdfDocumentReader pagePdfDocumentReader = new PagePdfDocumentReader(resource); // 只可以传pdf格式文件
        return pagePdfDocumentReader.read();
    }

    public List<Document> readPdfParagraph() {
        log.info("start read pdf file by paragraph");
        Resource resource = new DefaultResourceLoader().getResource(RagDocumentConstant.PDF_FILE_PATH);
        ParagraphPdfDocumentReader paragraphPdfDocumentReader = new ParagraphPdfDocumentReader(resource); // 有目录的pdf文件
        return paragraphPdfDocumentReader.read();
    }

    public List<Document> readMarkdown() {
        log.info("start read markdown file");
        MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(RagDocumentConstant.MARKDOWN_FILE_PATH); // 只可以传markdown格式文件
        return markdownDocumentReader.read();
    }

    public List<Document> readHtml() {
        log.info("start read html file");
        Resource resource = new DefaultResourceLoader().getResource(RagDocumentConstant.HTML_FILE_PATH);
        JsoupDocumentReader jsoupDocumentReader = new JsoupDocumentReader(resource); // 只可以传html格式文件
        return jsoupDocumentReader.read();
    }

    public List<Document> readTika(Resource resource) {
        log.info("start read file with Tika");
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource); // 可以传多种文档格式
        return tikaDocumentReader.read();
    }
}