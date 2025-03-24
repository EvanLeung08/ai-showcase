package com.evan.ai.service;

import com.evan.ai.provider.XunfeiProvider;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class BookService {
    private static final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();


    @Autowired
    private XunfeiProvider aiApiProvider;

    public String generateBookNote(String bookTitle, MultipartFile file) throws Exception {
        String template = readBookTemplate();

        // 添加PDF文件处理
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String content = stripper.getText(document);

            String prompt = template.replace("{bookTitle}", bookTitle)
                    .replace("{content}", content);
            String response = aiApiProvider.generateContent(
                    prompt,
                    "你是一位专业书评人，擅长深度解读书籍内容"
            );
            // Remove inline style and return the container div
            return "<div class='enhanced-book-notes'>" + convertMarkdownToHtml(response) + "</div>";
        }

    }


    private String convertMarkdownToHtml(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        return htmlRenderer.render(document);
    }

    private static String readBookTemplate() throws IOException {
        try (InputStream inputStream = BookService.class.getClassLoader()
                .getResourceAsStream("static/prompts/book_template.txt")) {
            byte[] encoded = inputStream.readAllBytes();
            return new String(encoded, StandardCharsets.UTF_8);
        }
    }
}
