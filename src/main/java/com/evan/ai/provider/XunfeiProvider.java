package com.evan.ai.provider;

import com.evan.ai.service.AutoPptGenerator;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Qualifier("xunfeiProvider")
@Service
@Primary
public class XunfeiProvider implements AiApiProvider {
    private static final String API_ENDPOINT = "https://spark-api-open.xf-yun.com/v1/chat/completions";

    public static String callDeepSeekApi(String words, String generationType) throws Exception {
        StringBuilder aggregatedContent = new StringBuilder();
        int part = 1;
        boolean hasMoreContent = true;

        String exampleTemplate = readExampleTemplate(generationType);

        while (hasMoreContent) {
            String requestBody = String.format("""
                            {
                                "model": "4.0Ultra",
                                "messages": [
                                    {"role": "system", "content": "你是一个单词记忆专家和世界记忆大师，你的任务是帮助用户记忆英语单词。"},
                                    {"role": "user", "content": "%s\\n\\n 请提供第%d部分输出。"}
                                ],
                                "stream": false,
                                "temperature": 0.7
                            }
                            """,
                    exampleTemplate.replace("\"", "\\\"")
                            .replace("\n", "\\n")
                            .replace("\\n-", "\\n-")
                            .replace("{{wordlist}}", words),
                    part);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(120))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + System.getProperty("API_KEY"))
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            int maxRetries = 3;
            int attempt = 0;
            while (attempt < maxRetries) {
                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    String responseBody = response.body();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String content = jsonResponse.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        log.info("Response: " + content);
                        aggregatedContent.append(content.replace("\\n", "\n"));
                        hasMoreContent = content.contains("请提供第" + (part + 1) + "部分");
                        part++;
                        break;
                    } catch (JSONException e) {
                        return responseBody;
                    }
                } catch (HttpTimeoutException e) {
                    attempt++;
                    if (attempt >= maxRetries) {
                        throw new Exception("Request timed out after " + maxRetries + " attempts", e);
                    }
                }
            }
        }
        return aggregatedContent.toString();
    }

    public static String callFortuneApi(String prompt, String systemRole) throws Exception {
        String requestBody = String.format("""
                            {
                                "model": "4.0Ultra",
                                "messages": [
                                    {"role": "system", "content": "%s"},
                                    {"role": "user", "content": "%s\\n\\n 请提供详细markdown报告输出。"}
                                ],
                                "stream": false,
                                "temperature": 0.7
                            }
                            """,
                systemRole.replace("\"", "\\\""),
                prompt.replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\\n-", "\\n-"),1);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(120))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + System.getProperty("API_KEY"))
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        JSONObject jsonResponse = new JSONObject(responseBody);
        String content = jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        return content;
    }

    private static String readExampleTemplate(String generationType) throws IOException {
        String templatePath = "static/prompts/story_template.txt";
        if ("mnemonics".equals(generationType)) {
            templatePath = "static/prompts/words_template.txt";
        }
        try (InputStream inputStream = AutoPptGenerator.class.getClassLoader().getResourceAsStream(templatePath)) {
            if (inputStream == null) {
                throw new IOException("File not found: " + templatePath);
            }
            byte[] encoded = inputStream.readAllBytes();
            return new String(encoded, StandardCharsets.UTF_8);
        }
    }

    @Override
    public String generateContent(String prompt, String systemRole) throws Exception {
        return callFortuneApi(prompt, systemRole);
    }

    @Override
    public String generateWithTemplate(String input, String templateType) throws Exception {
        return callDeepSeekApi(input, templateType);
    }
}