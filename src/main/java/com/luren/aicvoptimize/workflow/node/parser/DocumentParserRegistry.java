package com.luren.aicvoptimize.workflow.node.parser;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档解析器注册中心
 * <p>
 * 自动收集所有 {@link DocumentParser} 实现，根据文件后缀路由到对应解析器。
 * 新增解析器只需实现 {@link DocumentParser} 接口并注册为 Spring Bean，无需修改此类。
 *
 * @author lijianpan
 */
@Slf4j
@Component
public class DocumentParserRegistry {

    private final List<DocumentParser> parsers;
    private final Map<String, DocumentParser> parserMap = new HashMap<>();

    public DocumentParserRegistry(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    @PostConstruct
    public void init() {
        for (DocumentParser parser : parsers) {
            for (String ext : parser.supportedExtensions()) {
                String key = ext.toLowerCase();
                if (parserMap.containsKey(key)) {
                    log.warn("文件后缀 [{}] 存在多个解析器，后者将覆盖前者: {} -> {}",
                            key, parserMap.get(key).getClass().getSimpleName(), parser.getClass().getSimpleName());
                }
                parserMap.put(key, parser);
                log.info("注册文档解析器: {} -> {}", key, parser.getClass().getSimpleName());
            }
        }
        log.info("文档解析器注册完成，共支持 {} 种格式: {}", parserMap.size(), parserMap.keySet());
    }

    /**
     * 根据文件后缀获取对应的解析器
     *
     * @param file 文件
     * @return 匹配的解析器
     * @throws IllegalArgumentException 不支持的文件格式
     */
    public DocumentParser getParser(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("无法识别文件格式: " + filename);
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        DocumentParser parser = parserMap.get(ext);
        if (parser == null) {
            throw new IllegalArgumentException("不支持的文件格式: ." + ext + "，当前支持: " + parserMap.keySet());
        }
        return parser;
    }

    /**
     * 解析文件，自动路由到对应格式的解析器
     *
     * @param file 上传的文件
     * @return 提取的文本内容
     */
    public String parse(MultipartFile file) {
        DocumentParser parser = getParser(file);
        log.info("使用解析器 [{}] 处理文件: {}",
                parser.getClass().getSimpleName(), file.getOriginalFilename());
        return parser.parse(file);
    }
}
