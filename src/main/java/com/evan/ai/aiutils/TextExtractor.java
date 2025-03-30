package com.evan.ai.aiutils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TextExtractor {
    
    public static String extract(MultipartFile file) throws Exception {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("无法识别的文件类型");
        }

        return switch (contentType) {
            case "text/plain" -> extractTxt(file);
            case "application/pdf" -> extractPdf(file);
            case "text/markdown" -> extractMarkdown(file);
            default -> throw new IllegalArgumentException("不支持的文件类型: " + contentType);
        };
    }

    private static String extractTxt(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    private static String extractPdf(MultipartFile file) throws Exception {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private static String extractMarkdown(MultipartFile file) throws Exception {
        String raw = extractTxt(file);
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(parser.parse(raw));
    }
}