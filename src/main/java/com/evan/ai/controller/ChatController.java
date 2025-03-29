package com.evan.ai.controller;

import com.evan.ai.service.CopilotChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


import java.util.Map;

@RestController
public class ChatController {

    @Autowired
    private CopilotChatService chatService;

    @PostMapping("/copilot/chat")
    public ResponseEntity<?> handleChat(
            @RequestBody Map<String, String> request,
            HttpSession session) {

        String message = request.get("message");
        String token = session.getAttribute("access_token").toString();

        if (token == null) {
            return ResponseEntity.status(401).body("未授权");
        }

        String response = chatService.getCopilotResponse(message, token, session);
        return ResponseEntity.ok(Map.of("response", response));
    }

    @GetMapping("/copilot/new_chat")
    public ResponseEntity<?> startNewChat(HttpSession session) {
        String sessionId = session.getId();
        chatService.clearChatHistory(sessionId); // 替换原来的直接操作
        return ResponseEntity.ok("新对话已创建");
    }

}