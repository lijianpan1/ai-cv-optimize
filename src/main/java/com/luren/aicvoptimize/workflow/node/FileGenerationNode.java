package com.luren.aicvoptimize.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.luren.aicvoptimize.service.ResumeFileGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件生成节点
 * <p>
 * 读取工作流状态中的优化后简历内容，生成 PDF 和 Word 文件，
 * 并将文件下载地址写入状态供前端直接使用。
 *
 * @author lijianpan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileGenerationNode implements NodeAction {

    private static final String DOWNLOAD_URL_PREFIX = "/api/files/download/";

    private final ResumeFileGenerator resumeFileGenerator;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("=== 开始执行：文件生成节点 ===");

        String optimizedResume = (String) state.value("optimized_resume")
                .orElseThrow(() -> new IllegalArgumentException("缺少 optimized_resume，请先执行简历优化节点"));

        String baseName = (String) state.value("file_name").orElse(null);

        Map<String, String> filePaths = resumeFileGenerator.generateFiles(optimizedResume, baseName);

        // 提取文件名，构建下载 URL
        String pdfFileName = extractFileName(filePaths.get("pdf_path"));
        String wordFileName = extractFileName(filePaths.get("word_path"));

        String pdfDownloadUrl = DOWNLOAD_URL_PREFIX + encodeFileName(pdfFileName);
        String wordDownloadUrl = DOWNLOAD_URL_PREFIX + encodeFileName(wordFileName);

        log.info("文件生成完成 — PDF: {}, Word: {}", pdfDownloadUrl, wordDownloadUrl);

        Map<String, Object> result = new HashMap<>();
        result.put("pdf_path", filePaths.get("pdf_path"));
        result.put("word_path", filePaths.get("word_path"));
        result.put("pdf_download_url", pdfDownloadUrl);
        result.put("word_download_url", wordDownloadUrl);
        return result;
    }

    private String extractFileName(String filePath) {
        int lastSep = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSep >= 0 ? filePath.substring(lastSep + 1) : filePath;
    }

    private String encodeFileName(String fileName) {
        try {
            return URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
        } catch (Exception e) {
            return fileName;
        }
    }
}
