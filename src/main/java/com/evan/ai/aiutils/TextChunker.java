package com.evan.ai.aiutils;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TextChunker {
    private static final Pattern SENTENCE_END = Pattern.compile("[。！？.!?]");
    private static final int MIN_CHUNK_LENGTH = 50;

    public static List<String> chunk(String content, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        String[] paragraphs = content.split("\n\n+");

        for (String para : paragraphs) {
            String trimmed = para.replaceAll("\\s+", " ").trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.length() > maxChunkSize) {
                splitLongParagraph(trimmed, maxChunkSize, chunks);
            } else {
                if (buffer.length() + trimmed.length() > maxChunkSize) {
                    chunks.add(buffer.toString().trim());
                    buffer.setLength(0);
                }
                buffer.append(trimmed).append("\n");
            }
        }

        if (buffer.length() > 0) {
            chunks.add(buffer.toString().trim());
        }
        return chunks;
    }

    private static void splitLongParagraph(String text, int maxSize, List<String> chunks) {
        String[] sentences = SENTENCE_END.split(text);
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) continue;

            if (currentChunk.length() + trimmed.length() > maxSize) {
                if (currentChunk.length() >= MIN_CHUNK_LENGTH) {
                    chunks.add(currentChunk.toString());
                    currentChunk.setLength(0);
                }
            }
            currentChunk.append(trimmed).append("。");

            // 处理超长单句的情况
            if (trimmed.length() > maxSize * 2) {
                chunks.addAll(splitByLength(trimmed, maxSize));
                currentChunk.setLength(0);
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
    }

    private static List<String> splitByLength(String text, int size) {
        List<String> parts = new ArrayList<>();
        for (int i=0; i<text.length(); i+=size) {
            int end = Math.min(i + size, text.length());
            parts.add(text.substring(i, end));
        }
        return parts;
    }
}