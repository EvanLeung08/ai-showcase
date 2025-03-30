package com.evan.ai.service;

import com.evan.ai.configuration.AIConfig;
import com.evan.ai.configuration.CopilotConfig;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CopilotChatService {

    @Autowired
    private AIConfig config;

    @Autowired
    private CopilotConfig copilotConfig;

    @Autowired
    private RestTemplate restTemplate;
    // 在类中添加对话历史缓存
    private static final Map<String, List<Map<String, String>>> chatHistory = new ConcurrentHashMap<>();

    private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();

    // 修改清理方法，添加日志记录和双重清理
    public void clearChatHistory(String sessionId) {
        chatHistory.remove(sessionId);
        lastAccessTime.remove(sessionId); // 同时清理最后访问时间
        log.info("已清理会话历史: {}", sessionId);
    }

    // 在定时任务中调用清理逻辑
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        chatHistory.keySet().removeIf(sessionId -> {
            boolean expired = (currentTime - lastAccessTime.getOrDefault(sessionId, 0L)) > copilotConfig.getHistoryTtl() * 1000L;
            if (expired) {
                clearChatHistory(sessionId); // 通过统一方法清理
            }
            return expired;
        });
    }

    private void maintainHistorySize(String sessionId) {
        List<Map<String, String>> history = chatHistory.get(sessionId);
        if (history != null && history.size() > copilotConfig.getMaxHistory() * 2) {
            history = history.subList(history.size() - copilotConfig.getMaxHistory() * 2, history.size());
            chatHistory.put(sessionId, history);
        }
    }

    public String getCopilotResponse(String message, String accessToken, HttpSession session) {
        String copilotToken = null;
        if (session.getAttribute("copilot_token") == null ||
                session.getAttribute("copilot_token").toString().isEmpty()) {
            try {
                // 获取Copilot令牌
                HttpHeaders tokenHeaders = new HttpHeaders();
                tokenHeaders.setBearerAuth(accessToken);
           /*     tokenHeaders.add("Editor-Version", "Neovim/0.6.1");
                tokenHeaders.add("User-Agent", "GitHubCopilot/1.155.0");
                tokenHeaders.add("Editor-Plugin-Version", "copilot.vim/1.16.0");*/
                tokenHeaders.add("Accept", "application/json");
                ResponseEntity<Map> tokenResponse = restTemplate.exchange(
                        "https://api.github.com/copilot_internal/v2/token",
                        HttpMethod.GET,
                        new HttpEntity<>(tokenHeaders),
                        Map.class);

                copilotToken = tokenResponse.getBody().get("token").toString();
                session.setAttribute("copilot_token", copilotToken);
            } catch (Exception e) {
                log.error("获取Copilot令牌失败", e);
                session.setAttribute("copilot_token", null);
                return "你可能不是Github Copilot用户，请先开通Github Copilot! 详细信息: " + e.getMessage();
            }
        } else {
            copilotToken = session.getAttribute("copilot_token").toString();
        }

        if (copilotToken == null || copilotToken.isEmpty()) {
            return "用户未授权，请重新授权!";
        }

        List<Map<String, String>> history = chatHistory.computeIfAbsent(session.getId(), k -> new ArrayList<>());
        // 添加当前消息到历史
        history.add(Map.of("role", "user", "content", message));
        // 在保存新消息后维护历史长度（新增调用）
        maintainHistorySize(session.getId());
        // 添加上下文历史（保留最近5轮对话）
        if (history.size() > 10) {
            history = history.subList(history.size() - 5, history.size());
            chatHistory.put(session.getId(), history);
        }
        // 在获取历史记录后更新访问时间
        lastAccessTime.put(session.getId(), System.currentTimeMillis());

        // 使用配置的最大历史长度
        int maxHistory = copilotConfig.getMaxHistory();
        if (history.size() > maxHistory * 2) { // 保留最近N轮对话（用户和助手各一条为一轮）
            history = history.subList(history.size() - maxHistory * 2, history.size());
            chatHistory.put(session.getId(), history);
        }

        // 调用Copilot API
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(copilotToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        /*headers.add("Editor-Version", "vscode/1.93.1");
        headers.add("User-Agent", "GitHubCopilot/1.155.0");
        headers.add("Editor-Plugin-Version", "copilot-chat/0.20.3");*/
        headers.add("Editor-Version", "vscode/1.96.2");
        headers.add("User-Agent", "GitHubCopilot/1.270.0");
        headers.add("Editor-Plugin-Version", "copilot-chat/0.23.2");
        headers.add("Accept", "application/json");
        Map<String, Object> request = new HashMap<>();
        request.put("model", "gpt-4o-2024-05-13");//copilot-chat
        request.put("temperature", 0.5);
        request.put("max_tokens", 8192);
        request.put("messages", history.toArray()); // 传入完整对话历史

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
        String responseContent = messageObj.get("content").toString();
        // 添加助手响应到历史
        history.add(Map.of("role", "assistant", "content", responseContent));
        // 再次维护历史长度（新增调用）
        maintainHistorySize(session.getId());
        return responseContent;
    }

    // 在类中添加以下方法
    public Map<String, List<Map<String, String>>> getChatHistory() {
        return chatHistory;
    }

    // 在类中添加以下静态成员和方法
    private static CopilotChatService instance;

    @Autowired
    public CopilotChatService(AIConfig config, CopilotConfig copilotConfig, RestTemplate restTemplate) {
        this.config = config;
        this.copilotConfig = copilotConfig;
        this.restTemplate = restTemplate;
        instance = this;
    }

    public static CopilotChatService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("CopilotChatService 尚未初始化");
        }
        return instance;
    }

    public String getCompletion(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(getSystemToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("User-Agent", "evanai-autoppt/1.0");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4");
            requestBody.put("temperature", 0.2);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "你是一个严谨的知识问答助手"),
                    Map.of("role", "user", "content", prompt)
            ));

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    config.getCopilotApiUrl(),
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            return response.getBody().get("choices").toString();
        } catch (Exception e) {
            log.error("知识问答请求失败", e);
            return "答案生成失败: " + e.getMessage();
        }
    }

    // 在类中添加私有方法
    private String getSystemToken() {
        // 这里实现获取系统级访问令牌的逻辑
        return "sk-system-token-example";
    }


}