package com.evan.ai.service;

import com.evan.ai.configuration.AIConfig;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class CopilotChatService {

    @Autowired
    private AIConfig config;
    @Autowired
    private RestTemplate restTemplate;

    public String getCopilotResponse(String message, String accessToken, HttpSession session) {
        String copilotToken=null;
        if (session.getAttribute("copilot_token") == null ||
                session.getAttribute("copilot_token").toString().isEmpty()) {
            try {
                // 获取Copilot令牌
                HttpHeaders tokenHeaders = new HttpHeaders();
                tokenHeaders.setBearerAuth(accessToken);
                tokenHeaders.add("Editor-Version", "Neovim/0.6.1");
                tokenHeaders.add("User-Agent", "GitHubCopilot/1.155.0");
                tokenHeaders.add("Editor-Plugin-Version", "copilot.vim/1.16.0");
                tokenHeaders.add("Accept", "application/json");
                ResponseEntity<Map> tokenResponse = restTemplate.exchange(
                        "https://api.github.com/copilot_internal/v2/token",
                        HttpMethod.GET,
                        new HttpEntity<>(tokenHeaders),
                        Map.class);

                copilotToken = tokenResponse.getBody().get("token").toString();
                session.setAttribute("copilot_token", copilotToken);
            }catch (Exception e) {
                log.error("获取Copilot令牌失败", e);
                return "你可能不是Github Copilot用户，请先开通Github Copilot! 详细信息: " + e.getMessage();
            }
        }else {
            copilotToken = session.getAttribute("copilot_token").toString();
        }

        if(copilotToken ==null || copilotToken.isEmpty()) {
            return "用户未授权，请重新授权!";
        }
        // 调用Copilot API
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(copilotToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Editor-Version", "vscode/1.93.1");
        headers.add("User-Agent", "GitHubCopilot/1.155.0");
        headers.add("Editor-Plugin-Version", "copilot-chat/0.20.3");
        headers.add("Accept", "application/json");
        Map<String, Object> request = new HashMap<>();
        request.put("model", "copilot-chat");
        request.put("messages", new Object[]{Map.of("role", "user", "content", message)});

        ResponseEntity<Map> response = restTemplate.postForEntity(
                config.getCopilotApiUrl(),
                new HttpEntity<>(request, headers),
                Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("API响应格式异常");
        }

        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> messageObj = (Map<String, Object>) firstChoice.get("message");
        return messageObj.get("content").toString();
    }
}