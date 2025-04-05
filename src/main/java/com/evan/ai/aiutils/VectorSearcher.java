package com.evan.ai.aiutils;


import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class VectorSearcher {
    
    public static List<String> search(float[] queryVector, 
                                     List<DocumentVector> candidates,
                                     int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        // 使用优先队列维护TopK结果
        PriorityQueue<ScoredDocument> heap = new PriorityQueue<>(
            Comparator.comparingDouble(ScoredDocument::score)
        );

        for (DocumentVector doc : candidates) {
            float similarity = doc.similarity(queryVector);
            if (heap.size() < topK) {
                heap.offer(new ScoredDocument(doc, similarity));
            } else if (similarity > heap.peek().score()) {
                heap.poll();
                heap.offer(new ScoredDocument(doc, similarity));
            }
        }

        return heap.stream()
            .sorted((a, b) -> Float.compare(b.score(), a.score())) // 降序排序
            .map(d -> d.doc().getContent())
            .limit(topK)
            .collect(Collectors.toList());
    }

    private record ScoredDocument(DocumentVector doc, float score) {}
}