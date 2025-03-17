package com.evan.autoppt.provider;

import com.evan.autoppt.configuration.AIConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Qualifier("ollamaProvider")
public class OllamaProvider implements AiApiProvider {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private AIConfig config;

    @Override
    public String generateContent(String prompt, String systemRole) throws Exception {
        String jsonBody = String.format("""
                        {
                            "model": "%s",
                            "prompt": "%s",
                            "system": "%s",
                            "stream": false
                        }""",
                config.getOllamaModel(),
                prompt.replace("\"", "\\\""),
                systemRole.replace("\"", "\\\""));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                config.getOllamaEndpoint(),
                HttpMethod.POST,
                new HttpEntity<>(jsonBody, headers),
                String.class
        );

        JSONObject jsonResponse = new JSONObject(response.getBody());
        return jsonResponse.getString("response");
    }

    @Override
    public String generateWithTemplate(String input, String templateType) throws Exception {
       return null;
    }


}