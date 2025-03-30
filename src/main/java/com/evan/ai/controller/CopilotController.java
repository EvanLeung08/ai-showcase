package com.evan.ai.controller;

import com.evan.ai.configuration.AIConfig;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;


import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class CopilotController {

    @Autowired
    private AIConfig config;
    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/copilot/auth")
    public ResponseEntity<?> startAuth(HttpSession session) {
        // 获取GitHub设备授权码
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", config.getGithubClientId());
        params.add("scope", "read:user user:email");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
       /* headers.add("Editor-Version", "Neovim/0.6.1");
        headers.add("User-Agent", "GitHubCopilot/1.155.0");
        headers.add("Editor-Plugin-Version", "copilot.vim/1.16.0");*/
        headers.add("Accept", "application/json");
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://github.com/login/device/code",
                new HttpEntity<>(params, headers),
                Map.class);

        Map<String, String> result = response.getBody();
        session.setAttribute("client_id", config.getGithubClientId());
        session.setAttribute("device_code", result.get("device_code"));

        return ResponseEntity.ok(Map.of(
                "userCode", result.get("user_code"),
                "verificationUri", result.get("verification_uri")
        ));
    }

    @GetMapping("/copilot/callback")
    public ResponseEntity<?> handleCallback(
            HttpSession session) {
        if(session.getAttribute("access_token")!=null&& !session.getAttribute("access_token").toString().isEmpty()) {
            return ResponseEntity.ok(Map.of("status", "authorized"));
        }
        try {
            // 使用设备码获取访问令牌
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", session.getAttribute("client_id").toString());
            params.add("device_code", session.getAttribute("device_code").toString());
            params.add("grant_type", "urn:ietf:params:oauth:grant-type:device_code");

            HttpHeaders headers = new HttpHeaders();
           /* headers.add("Editor-Version", "Neovim/0.6.1");
            headers.add("User-Agent", "GitHubCopilot/1.155.0");
            headers.add("Editor-Plugin-Version", "copilot.vim/1.16.0");*/
            headers.add("Accept", "application/json");
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://github.com/login/oauth/access_token",
                    new HttpEntity<>(params, headers),
                    Map.class);

            Map<String, String> tokenResponse = response.getBody();
            if (tokenResponse == null || tokenResponse.get("access_token") == null || tokenResponse.get("access_token").isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("授权失败");
            }
            session.setAttribute("access_token", tokenResponse.get("access_token"));

            return ResponseEntity.ok(Map.of("status", "authorized"));
        }catch (Exception ex){
            log.error("获取授权失败", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取授权失败");
        }
    }
}