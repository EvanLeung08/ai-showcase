package com.evan.ai.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "copilot")
public class CopilotConfig {
    private int maxHistory;
    private int historyTtl;
}