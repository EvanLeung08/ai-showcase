package com.evan.autoppt.controller;

import com.evan.autoppt.service.FortuneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class FortuneController {
    
    @Autowired
    private FortuneService fortuneService;

    @PostMapping("/generateFortune")
    public Map<String, String> generateFortune(@RequestBody Map<String, String> request) {
        String report = fortuneService.generateFortuneReport(
            request.get("name"),
            request.get("age"),
            request.get("birthdate")
        );
        return Map.of("report", report);
    }
}