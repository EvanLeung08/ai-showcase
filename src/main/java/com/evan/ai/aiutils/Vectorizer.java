package com.evan.ai.aiutils;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Vectorizer {
    private static Vectorizer instance;

    @Value("${ai.vectorization.model:local}")
    private String modelType;

    // 本地模型使用的词向量维度（示例值）
    private static final int DIMENSIONS = 1536;


    // 移除单例模式相关代码
    public Vectorizer() {
        // 添加默认值保护
        if (modelType == null) {
            modelType = "local";
            System.out.println("WARN: Using default vectorization model");
        }
    }

    public List<float[]> vectorize(List<String> texts) {
        return switch (modelType.toLowerCase()) {
            case "openai" -> {
                if (texts.stream().anyMatch(t -> t.length() > 8192)) {
                    throw new IllegalArgumentException("OpenAI模型输入长度限制为8192字符");
                }
                yield vectorizeWithOpenAI(texts);
            }
            case "local" -> vectorizeLocally(texts);
            default -> throw new IllegalArgumentException("Unsupported vectorization model: " + modelType);
        };
    }

    // 修改本地向量化方法
    private List<float[]> vectorizeLocally(List<String> texts) {
        List<float[]> vectors = new ArrayList<>();
        for (String text : texts) {
            float[] vector = new float[DIMENSIONS]; // 明确使用768维度
            // 将double转换为float
            float[] floatVector = new float[DIMENSIONS];
            for (int i = 0; i < DIMENSIONS; i++) {
                floatVector[i] = (float) Math.random(); // 显式转换为float
            }
            vectors.add(floatVector);
        }
        return vectors;
    }

    // 修改OpenAI向量化方法
    private List<float[]> vectorizeWithOpenAI(List<String> texts) {
        List<float[]> vectors = new ArrayList<>();
        for (String text : texts) {
            // 直接创建float数组
            float[] floatVector = new float[1536];
            for (int i = 0; i < 1536; i++) {
                floatVector[i] = (float) Math.random();
            }
            vectors.add(floatVector);
        }
        return vectors;
    }

    public float[] vectorize(String text) {
        return vectorize(List.of(text)).get(0);
    }
}