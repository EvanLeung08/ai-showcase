package com.evan.ai.provider;

public interface AiApiProvider {
    String generateContent(String prompt, String systemRole) throws Exception;

    String generateWithTemplate(String input, String templateType) throws Exception;
}