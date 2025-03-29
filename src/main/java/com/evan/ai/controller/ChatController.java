package com.evan.ai.controller;

import com.evan.ai.service.CopilotChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
}