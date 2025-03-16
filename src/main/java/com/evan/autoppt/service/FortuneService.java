package com.evan.autoppt.service;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class FortuneService {
    // 在类声明后添加渲染器实例
    private static final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    public String generateFortuneReport(String name, String age, String birthdate) {
        try {
            String template = readExampleTemplate();
            String formattedPrompt = template.replace("{name}", name)
                    .replace("{age}", age)
                    .replace("{birthdate}", birthdate);
            // 修改API调用参数
            String response = AutoPptGenerator.callFortuneApi(
                    formattedPrompt.replace("\"", "\\\""),
                    "你是一位资深命理学家，精通八字、紫微斗数和现代心理学"
            );
            // 将markdown转换为HTML
            return convertMarkdownToHtml(response);
        } catch (Exception e) {
            throw new RuntimeException("生成命理报告失败", e);
        }
    }

    private String convertMarkdownToHtml(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        return htmlRenderer.render(document); // 现在可以正确解析
    }

    private static String readExampleTemplate() throws IOException {
        String templatePath = "static/prompts/fortune_template.txt";

        try (InputStream inputStream = AutoPptGenerator.class.getClassLoader().getResourceAsStream(templatePath)) {
            if (inputStream == null) {
                throw new IOException("File not found: " + templatePath);
            }
            byte[] encoded = inputStream.readAllBytes();
            return new String(encoded, StandardCharsets.UTF_8);
        }
    }
}