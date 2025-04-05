package com.evan.ai.aiutils;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentVector {
    private String content;
    private float[] vector;

    // 计算余弦相似度
    public float similarity(float[] other) {
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < vector.length; i++) {
            dot += vector[i] * other[i];
            normA += vector[i] * vector[i];
            normB += other[i] * other[i];
        }
        return dot / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }
}