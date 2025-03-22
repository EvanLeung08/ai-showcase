package com.evan.ai.controller;

import com.evan.ai.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class TranslationController {

    @Autowired
    private TranslationService translationService;

    @PostMapping("/translate")
    public Map<String, String> translate(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            String sourceLang = request.get("sourceLang");
            String targetLang = request.get("targetLang");
            String translatedText = translationService.translate(text, sourceLang, targetLang);
            return Map.of("translatedText", translatedText);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}