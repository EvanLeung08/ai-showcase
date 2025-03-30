package com.evan.ai.controller;

import com.evan.ai.service.KnowledgeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;
    // 修改文件校验逻辑
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "md", "pdf");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "text/plain",
            "text/markdown",
            "application/pdf",
            "application/octet-stream"  // 允许浏览器自动检测的类型
    );

    @PostMapping("/knowledge/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("files") MultipartFile[] files,  // ✅ 接收多文件参数
            HttpSession session) {

        String sessionId = session.getId();
        try {
            // 新增多文件遍历处理
            for (MultipartFile file : files) {
                // 扩展名校验
                String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
                if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                    return ResponseEntity.badRequest().body("不支持的文件扩展名: " + extension);
                }

                // MIME类型校验
                String contentType = file.getContentType();
                if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
                    return ResponseEntity.badRequest().body("不支持的文件类型: " + contentType);
                }

                knowledgeService.processDocument(file, session);  // ✅ 处理每个文件
            }

            return ResponseEntity.ok(Map.of("status", "processing"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 问答接口（复用Copilot授权）
    @PostMapping("/knowledge/ask")
    public ResponseEntity<?> handleQuestion(
            @RequestBody Map<String, String> request,
            HttpSession session) {

        String question = request.get("question");
        String sessionId = session.getId();

         if (!knowledgeService.isKnowledgeReady(sessionId)) {
            return ResponseEntity.badRequest().body("请先上传知识文档");
        }

        String answer = knowledgeService.getAnswer(question, session);
        return ResponseEntity.ok(Map.of("answer", answer));
    }

}