package com.evan.ai.sample;

import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionAnswerer {
    // 存储文档片段和对应的向量
    static Map<String, List<Double>> documentEmbeddings = new HashMap<>();

    public static void main(String[] args) throws IOException {


        String apiKey = "";

        System.out.println("开始整理文档！");
        // 1. 读取你的文档文件（假设是文本文件）
        String docContent = Files.readString(Paths.get("your_document.txt"));

        // 2. 将文档拆分成段落（按换行符拆分，实际可按需求优化）
        String[] paragraphs = docContent.split("\n\n");

        // 3. 为每个段落生成嵌入向量
        for (String paragraph : paragraphs) {
            List<Double> embedding = getEmbedding(apiKey, paragraph);
            documentEmbeddings.put(paragraph, embedding);
        }
        System.out.println("文档预处理完成！");

        String question = "我的文档中提到了哪些关键政策？";

        // 1. 获取问题的向量
        List<Double> questionEmbedding = getEmbedding(apiKey, question);

        // 2. 找到最相关的文档段落（余弦相似度）
        String mostRelevantDoc = findMostRelevantDoc(questionEmbedding);

        // 3. 调用 Chat API，附带文档内容
        String answer = askChatGPT(apiKey, question, mostRelevantDoc);
        System.out.println("答案: " + answer);
    }

    // 计算余弦相似度（简化版）
    private static double cosineSimilarity(List<Double> vec1, List<Double> vec2) {
        double dotProduct = 0, norm1 = 0, norm2 = 0;
        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += Math.pow(vec1.get(i), 2);
            norm2 += Math.pow(vec2.get(i), 2);
        }
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    // 查找最相关文档
    private static String findMostRelevantDoc(List<Double> questionEmbedding) {
        String bestDoc = "";
        double maxSimilarity = -1;
        for (Map.Entry<String, List<Double>> entry : documentEmbeddings.entrySet()) {
            double similarity = cosineSimilarity(questionEmbedding, entry.getValue());
            System.out.println("Similarity with document: " + similarity); // Debug print
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestDoc = entry.getKey();
            }
        }
        System.out.println("Max similarity: " + maxSimilarity); // Debug print
        return bestDoc;
    }

    // 调用 Chat API
    private static String askChatGPT(String apiKey, String question, String context) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String prompt = "请根据以下文档内容回答问题：\n【文档内容】\n" + context + "\n\n【问题】\n" + question;

        // Create JSON object using Gson
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("model", "gpt-4o-2024-05-13");

        JsonArray messagesArray = new JsonArray();
        JsonObject messageObject = new JsonObject();
        messageObject.addProperty("role", "user");
        messageObject.addProperty("content", prompt);
        messagesArray.add(messageObject);

        jsonObject.add("messages", messagesArray);

        String jsonBody = jsonObject.toString();
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.githubcopilot.com/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Editor-Version", "vscode/1.96.2")
                .addHeader("User-Agent", "GitHubCopilot/1.270.0")
                .addHeader("Editor-Plugin-Version", "copilot-chat/0.23.2")
                .addHeader("Accept", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            System.out.println(responseBody);
            // Parse JSON to extract the answer (simplified here)
            return parseAnswerFromJson(responseBody);
        }
    }

    // 解析 OpenAI 的响应，提取回答内容
    private static String parseAnswerFromJson(String jsonResponse) {
        try {
            // 1. 将 JSON 字符串解析为 JsonObject
            JsonObject root = new Gson().fromJson(jsonResponse, JsonObject.class);

            // 2. 获取 choices 数组
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                return "未找到回答内容";
            }

            // 3. 提取第一个 choice 中的 message.content
            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");
            String content = message.get("content").getAsString();

            return content;
        } catch (Exception e) {
            e.printStackTrace();
            return "解析回答失败";
        }
    }

    // 调用 Embeddings API 获取向量
    public static List<Double> getEmbedding(String apiKey, String text) throws IOException {
        OkHttpClient client = new OkHttpClient();

        // Create JSON object using Gson
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("model", "copilot-text-embedding-ada-002");

        JsonArray inputArray = new JsonArray();
        inputArray.add(text);
        jsonObject.add("input", inputArray);

        String jsonBody = jsonObject.toString();
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.githubcopilot.com/embeddings")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("x-request-id", "5883a6a7-1995-4036-899d-b268ffbe2b26")
                .addHeader("vscode-sessionid", "731b4209-8f5d-4864-b56c-4bdf021fec011743340560435015")
                .addHeader("vscode-machineid", "")
                .addHeader("copilot-integration-id", "vscode-chat")
                .addHeader("openai-organization", "github-copilot")
                .addHeader("openai-intent", "conversation-panel")
                .addHeader("User-Agent", "github.com/stong1994/github-copilot-api/1.0.0")
                .addHeader("Client-Version", "1.0.0")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            System.out.println(responseBody);
            // Parse JSON to extract the embedding (simplified here)
            return parseEmbeddingFromJson(responseBody);
        }
    }

    // 简化的 JSON 解析（实际建议用 Gson/Jackson）
    private static List<Double> parseEmbeddingFromJson(String json) {
        List<Double> embedding = new ArrayList<>();
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        JsonArray dataArray = jsonObject.getAsJsonArray("data");
        if (dataArray != null && dataArray.size() > 0) {
            JsonObject firstDataObject = dataArray.get(0).getAsJsonObject();
            JsonArray embeddingArray = firstDataObject.getAsJsonArray("embedding");
            for (JsonElement element : embeddingArray) {
                embedding.add(element.getAsDouble());
            }
        }
        return embedding;
    }
}