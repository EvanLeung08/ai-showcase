package com.evan.ai.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIConfig {
    private String provider;
    private String ollamaModel;
    private String ollamaEndpoint;
    private String githubClientId;
    private String githubClientSecret;
    private String copilotApiUrl = "https://api.githubcopilot.com/chat/completions";//embeddings
    // 保持原有配置不变的情况下新增
    private String embeddingModel = "copilot-text-embedding-ada-002";

}