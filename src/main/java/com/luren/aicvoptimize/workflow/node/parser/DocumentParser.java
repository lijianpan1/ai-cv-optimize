package com.luren.aicvoptimize.workflow.node.parser;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文档解析策略接口
 * <p>
 * 每种文件格式实现一个解析器，通过 Spring 自动注册到 {@link DocumentParserRegistry}。可。
 *
 * @author lijianpan
 */
public interface DocumentParser {

    /**
     * @return 支持的文件后缀列表（不含点号，
     *  * 新增格式只需实现此接口并标注 {@link org.springframework.stereotype.Component} 即小写），如 ["pdf", "docx"]
     */
    String[] supportedExtensions();

    /**
     * 解析文件并提取文本内容
     *
     * @param file 上传的文件
     * @return 提取的文本内容
     */
    String parse(MultipartFile file);
}
