package com.evan.autoppt.controller;

import com.evan.autoppt.service.PptService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Controller
public class PptController {

    @Autowired
    private PptService pptService;

    @PostMapping("/generate")
    public void generatePpt(@RequestParam("prompt") String prompt, HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        response.setHeader("Content-Disposition", "attachment; filename=output.pptx");
        try {
            String decodedPrompt = URLDecoder.decode(prompt, StandardCharsets.UTF_8.name());
            pptService.generatePpt(decodedPrompt, response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/preview")
    @ResponseBody
    public List<String> previewPpt(@RequestParam("prompt") String prompt) {
        try {
            String decodedPrompt = URLDecoder.decode(prompt, StandardCharsets.UTF_8.name());
            List<byte[]> images = pptService.convertPptToImages(decodedPrompt);
            List<String> base64Images = new ArrayList<>();
            for (byte[] image : images) {
                base64Images.add(Base64.getEncoder().encodeToString(image));
            }
            return base64Images;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}