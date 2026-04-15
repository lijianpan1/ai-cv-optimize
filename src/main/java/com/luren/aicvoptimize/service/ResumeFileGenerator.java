package com.luren.aicvoptimize.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 简历文件生成服务
 * <p>
 * 将优化后的 Markdown 简历内容导出为 PDF 和 Word (.docx) 文件。
 *
 * @author lijianpan
 */
@Slf4j
@Service
public class ResumeFileGenerator {

    @Value("${app.output-dir:./output/resumes}")
    private String outputDir;

    @PostConstruct
    public void init() {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("简历文件输出目录已创建: {}", dir.getAbsolutePath());
            }
        }
    }

    /**
     * 同时生成 PDF 和 Word 文件
     *
     * @param markdownContent Markdown 格式的简历内容
     * @param baseName        基础文件名（不含扩展名），传 null 则自动生成 UUID
     * @return 包含 pdf_path 和 word_path 的 Map
     */
    public Map<String, String> generateFiles(String markdownContent, String baseName) {
        String name = (baseName != null && !baseName.isBlank()) ? baseName : "resume_" + System.currentTimeMillis();
        String pdfPath = generatePdf(markdownContent, name);
        String wordPath = generateWord(markdownContent, name);
        return Map.of("pdf_path", pdfPath, "word_path", wordPath);
    }

    /**
     * 生成 PDF 文件
     */
    public String generatePdf(String markdownContent, String baseName) {
        String filePath = resolvePath(baseName, ".pdf");
        try {
            ensureParentDir(filePath);

            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font headingFont = new Font(baseFont, 14, Font.BOLD);
            Font subHeadingFont = new Font(baseFont, 12, Font.BOLD);
            Font normalFont = new Font(baseFont, 11, Font.NORMAL);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                Document document = new Document();
                PdfWriter.getInstance(document, fos);
                document.open();

                List<String> lines = markdownContent.lines().toList();
                for (String rawLine : lines) {
                    String line = rawLine.trim();

                    if (line.startsWith("# ")) {
                        String text = line.substring(2).trim();
                        Paragraph p = new Paragraph(text, titleFont);
                        p.setAlignment(Element.ALIGN_CENTER);
                        p.setSpacingAfter(12);
                        document.add(p);
                    } else if (line.startsWith("## ")) {
                        String text = line.substring(3).trim();
                        addSectionDivider(document, baseFont);
                        Paragraph p = new Paragraph(text, headingFont);
                        p.setSpacingBefore(10);
                        p.setSpacingAfter(6);
                        document.add(p);
                    } else if (line.startsWith("### ")) {
                        String text = line.substring(4).trim();
                        Paragraph p = new Paragraph(text, subHeadingFont);
                        p.setSpacingBefore(8);
                        p.setSpacingAfter(4);
                        document.add(p);
                    } else if (line.startsWith("- ") || line.startsWith("* ")) {
                        String text = line.substring(2).trim();
                        Paragraph p = new Paragraph("• " + text, normalFont);
                        p.setIndentationLeft(20);
                        p.setSpacingAfter(3);
                        document.add(p);
                    } else if (line.isEmpty()) {
                        document.add(new Paragraph("", normalFont));
                    } else {
                        Paragraph p = new Paragraph(line, normalFont);
                        p.setSpacingAfter(3);
                        document.add(p);
                    }
                }

                document.close();
            }

            log.info("PDF 文件生成成功: {}", filePath);
            return filePath;
        } catch (DocumentException | IOException e) {
            // 生成失败时清理残留文件
            tryDeleteFile(filePath);
            throw new RuntimeException("生成 PDF 文件失败: " + filePath, e);
        }
    }

    /**
     * 生成 Word 文件
     */
    public String generateWord(String markdownContent, String baseName) {
        String filePath = resolvePath(baseName, ".docx");
        try {
            ensureParentDir(filePath);

            try (XWPFDocument doc = new XWPFDocument()) {
                List<String> lines = markdownContent.lines().toList();

                for (String rawLine : lines) {
                    String line = rawLine.trim();

                    if (line.startsWith("# ")) {
                        // 一级标题
                        String text = line.substring(2).trim();
                        XWPFParagraph p = doc.createParagraph();
                        p.setAlignment(ParagraphAlignment.CENTER);
                        XWPFRun run = p.createRun();
                        run.setText(text);
                        run.setBold(true);
                        run.setFontSize(20);
                        run.setFontFamily("微软雅黑");
                        p.setSpacingAfter(200);
                    } else if (line.startsWith("## ")) {
                        // 二级标题 — 带下划线分隔
                        String text = line.substring(3).trim();
                        addWordSectionDivider(doc);
                        XWPFParagraph p = doc.createParagraph();
                        XWPFRun run = p.createRun();
                        run.setText(text);
                        run.setBold(true);
                        run.setFontSize(14);
                        run.setFontFamily("微软雅黑");
                        p.setSpacingBefore(200);
                        p.setSpacingAfter(100);
                    } else if (line.startsWith("### ")) {
                        // 三级标题
                        String text = line.substring(4).trim();
                        XWPFParagraph p = doc.createParagraph();
                        XWPFRun run = p.createRun();
                        run.setText(text);
                        run.setBold(true);
                        run.setFontSize(12);
                        run.setFontFamily("微软雅黑");
                        p.setSpacingBefore(120);
                        p.setSpacingAfter(60);
                    } else if (line.startsWith("- ") || line.startsWith("* ")) {
                        // 列表项
                        String text = line.substring(2).trim();
                        XWPFParagraph p = doc.createParagraph();
                        p.setIndentationLeft(400);
                        XWPFRun run = p.createRun();
                        run.setText("• " + text);
                        run.setFontSize(11);
                        run.setFontFamily("宋体");
                        p.setSpacingAfter(40);
                    } else if (line.isEmpty()) {
                        // 空行，跳过
                        continue;
                    } else {
                        // 普通文本
                        XWPFParagraph p = doc.createParagraph();
                        XWPFRun run = p.createRun();
                        run.setText(line);
                        run.setFontSize(11);
                        run.setFontFamily("宋体");
                        p.setSpacingAfter(40);
                    }
                }

                try (FileOutputStream out = new FileOutputStream(filePath)) {
                    doc.write(out);
                }
            }

            log.info("Word 文件生成成功: {}", filePath);
            return filePath;
        } catch (IOException e) {
            tryDeleteFile(filePath);
            throw new RuntimeException("生成 Word 文件失败: " + filePath, e);
        }
    }

    // ==================== 内部方法 ====================

    private void tryDeleteFile(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException ignored) {
            log.warn("清理残留文件失败: {}", filePath);
        }
    }

    /**
     * 根据文件名解析完整文件路径（供下载接口使用）
     */
    public Path resolveFilePath(String fileName) {
        return Path.of(outputDir, fileName);
    }

    private String resolvePath(String baseName, String extension) {
        return Path.of(outputDir, baseName + extension).toString();
    }

    private void ensureParentDir(String filePath) throws IOException {
        Path parent = Path.of(filePath).getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    /**
     * PDF 中添加章节分隔线
     */
    private void addSectionDivider(Document document, BaseFont baseFont) throws IOException {
        Font lineFont = new Font(baseFont, 6, Font.NORMAL);
        Paragraph divider = new Paragraph("────────────────────────────────", lineFont);
        divider.setAlignment(Element.ALIGN_CENTER);
        divider.setSpacingBefore(6);
        divider.setSpacingAfter(2);
        document.add(divider);
    }

    /**
     * Word 中添加章节分隔线（用底部边框模拟）
     */
    private void addWordSectionDivider(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.setBorderBottom(Borders.SINGLE);
        p.setSpacingAfter(60);
    }
}
