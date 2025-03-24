package com.evan.ai.controller;

import com.evan.ai.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
public class BookController {

    @Autowired
    private BookService bookService;

    @PostMapping("/generateBookNote")
    public Map<String, String> generateBookNote(
            @RequestHeader("X-Book-Title") String encodedTitle,
            @RequestParam("bookFile") MultipartFile bookFile) throws Exception {

        // 添加文件类型校验
        if (!Objects.requireNonNull(bookFile.getContentType()).equalsIgnoreCase("application/pdf")) {
            throw new IllegalArgumentException("仅支持PDF文件: " + bookFile.getOriginalFilename());
        }

        String bookTitle = URLDecoder.decode(encodedTitle, StandardCharsets.UTF_8);
        String report = bookService.generateBookNote(bookTitle, bookFile);

        return Map.of(
                "report", report,
                "progress", "COMPLETE" // 实际应为分块处理进度
        );
    }

    @PostMapping("/uploadDocument")
    public Map<String, Object> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "chunkNumber", defaultValue = "0") int chunkNumber,
            @RequestParam(value = "totalChunks", defaultValue = "1") int totalChunks) {

        try {
            // 分片处理逻辑
            byte[] bytes = file.getBytes();
            String content = new String(bytes, StandardCharsets.UTF_8);

            return Map.of(
                "status", "success",
                "chunk", chunkNumber,
                "content", content
            );
        } catch (IOException e) {
            return Map.of(
                "status", "error",
                "message", "文件上传失败: " + e.getMessage()
            );
        }
    }
}
