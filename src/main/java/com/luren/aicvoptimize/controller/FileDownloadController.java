package com.luren.aicvoptimize.controller;

import com.luren.aicvoptimize.service.ResumeFileGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

import static java.net.URLEncoder.encode;

/**
 * 文件下载 REST API
 * <p>
 * 提供简历生成文件的下载服务（PDF / Word），
 * 仅允许下载 output-dir 目录下的文件，防止路径穿越。
 *
 * @author lijianpan
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/files")
public class FileDownloadController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx");

    private final ResumeFileGenerator resumeFileGenerator;

    /**
     * 下载简历文件
     *
     * @param fileName 文件名（如 resume_1740xxxxxx.pdf 或 resume_1740xxxxxx.docx）
     * @return 文件二进制流
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> download(@PathVariable String fileName) {
        // 对路径变量进行 URL 解码，支持中文文件名
        String decodedFileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

        // 安全校验：文件名不允许包含路径分隔符
        if (decodedFileName.contains("..") || decodedFileName.contains("/") || decodedFileName.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        String extension = getExtension(decodedFileName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest().build();
        }

        Path filePath = resumeFileGenerator.resolveFilePath(decodedFileName);
        if (!java.nio.file.Files.exists(filePath)) {
            log.warn("文件不存在: {}", filePath);
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = resolveContentType(extension);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encode(decodedFileName, "UTF-8") + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            log.error("文件路径异常: {}", decodedFileName, e);
            return ResponseEntity.internalServerError().build();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex) : "";
    }

    private String resolveContentType(String extension) {
        switch (extension) {
            case ".pdf":
                return "application/pdf";
            case ".docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default:
                return "application/octet-stream";
        }
    }
}
