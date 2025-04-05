package com.evan.ai.service;

import com.evan.ai.aiutils.*;
import com.evan.ai.configuration.AIConfig;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class KnowledgeService {
    // 新增依赖注入
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AIConfig aiConfig;

    @Autowired
    private CopilotChatService copilotChatService;
    @Autowired  // 添加在类顶部
    private Vectorizer vectorizer;

    // 存储各会话的知识库（可替换为真实数据库）
    private Map<String, List<DocumentVector>> knowledgeBase = new ConcurrentHashMap<>();

    public void processDocument(MultipartFile file, HttpSession session) throws Exception {
        String sessionId = session.getId();
        // 文本提取
        String content = TextExtractor.extract(file);

        // 文本分块
        List<String> chunks = TextChunker.chunk(content, 500);

        // 替换向量化实现
        List<float[]> vectors = new ArrayList<>();
        for (String chunk : chunks) {
            List<Double> embedding = getEmbeddingFromAPI(chunk, session);
            // 转换Double为float数组
            float[] floatArray = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                floatArray[i] = embedding.get(i).floatValue();
            }
            vectors.add(floatArray);
        }
        // 存储到会话知识库
        List<DocumentVector> docs = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            docs.add(new DocumentVector(chunks.get(i), vectors.get(i)));
        }
        knowledgeBase.put(sessionId, docs);
    }

    private List<Double> getEmbeddingFromAPI(String text, HttpSession session) throws Exception {
        String copilotToken = (String) session.getAttribute("copilot_token");
        if (copilotToken == null) {
            throw new IllegalStateException("未找到Copilot访问令牌");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(copilotToken);
        headers.add("User-Agent", "GitHubCopilot/1.270.0");
        headers.add("Accept", "application/json");
        headers.add("x-request-id", "5883a6a7-1995-4036-899d-b268ffbe2b26");
        headers.add("vscode-sessionid", "731b4209-8f5d-4864-b56c-4bdf021fec011743340560435015");
        headers.add("vscode-machineid", "");
        headers.add("copilot-integration-id", "vscode-chat");
        headers.add("openai-organization", "github-copilot");
        headers.add("openai-intent", "conversation-panel");
        headers.add("User-Agent", "github.com/stong1994/github-copilot-api/1.0.0");
        headers.add("Client-Version", "1.0.0");
        headers.add("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "copilot-text-embedding-ada-002");
        requestBody.put("input", List.of(text));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                aiConfig.getCopilotApiUrl().replace("chat/completions", "embeddings"),
                new HttpEntity<>(requestBody, headers),
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            return parseEmbedding(response.getBody());
        }
        throw new RuntimeException("Embedding API请求失败: " + response.getStatusCode());
    }

    private List<Double> parseEmbedding(Map<String, Object> response) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        List<Double> embedding = (List<Double>) data.get(0).get("embedding");
        return embedding;
    }

    public String getAnswer(String question, HttpSession session) {
        try {
            // 1. 问题向量化（使用依赖注入的vectorizer）
            // 1. 使用API向量化问题
            List<Double> embedding = getEmbeddingFromAPI(question, session);
            float[] qVector = convertToFloatArray(embedding);


            // 2. 获取知识库数据
            List<DocumentVector> candidates = knowledgeBase.get(session.getId());
            if (candidates == null || candidates.isEmpty()) {
                throw new IllegalStateException("未找到会话知识库：" + session.getId());
            }

            // 3. 相似度检索（添加空结果保护）
            List<String> contexts = VectorSearcher.search(qVector, candidates, 3);
            if (contexts.isEmpty()) {
                return "未找到相关上下文信息";
            }

            // 4. 构造带上下文的Prompt
            String prompt = buildPrompt(question, contexts);

            // 5. 复用Copilot基础设施（添加异常处理）
            String accessToken = (String) session.getAttribute("access_token");
            return copilotChatService.getCopilotResponse(prompt, accessToken, session);
        } catch (Exception e) {
            log.error("知识问答失败 sessionId={}", session.getId(), e);
            return "系统繁忙，请稍后再试。错误详情：" + e.getMessage();
        }
    }

    // 在类中添加以下方法（放在getAnswer方法附近）
    private float[] convertToFloatArray(List<Double> embedding) {
        float[] floats = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            floats[i] = embedding.get(i).floatValue();
        }
        return floats;
    }

    private String buildPrompt(String question, List<String> contexts) {
        return String.format("基于以下知识回答问题：\n%s\n\n问题：%s",
                String.join("\n", contexts), question);
    }

    // 在类中添加以下方法
    public boolean isKnowledgeReady(String sessionId) {
        List<DocumentVector> docs = knowledgeBase.get(sessionId);
        return docs != null && !docs.isEmpty();
    }
}