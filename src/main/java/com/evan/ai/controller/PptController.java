package com.evan.ai.controller;

import com.evan.ai.service.ImageGenerationService;
import com.evan.ai.service.PptService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Controller
public class PptController {

    @Autowired
    private PptService pptService;

    @Autowired
    private ImageGenerationService imageGenerationService;

    // 在现有代码后面新增以下方法
    @PostMapping("/generateImage")
    @ResponseBody
    public Map<String, Object> generateImage(@RequestParam("prompt") String prompt) {
        Map<String, Object> response = new HashMap<>();
        try {
            String decodedPrompt = URLDecoder.decode(prompt, StandardCharsets.UTF_8.name());
            String base64Image = imageGenerationService.generateImage(decodedPrompt);
            response.put("success", true);
            response.put("image", base64Image);
        } catch (Exception e) {
            log.error("图像生成失败", e);
            response.put("success", false);
            response.put("message", "图像生成失败: " + e.getMessage());
        }
        return response;
    }
    // PptController.java
    @PostMapping("/preview")
    @ResponseBody
    public List<String> previewPpt(@RequestParam("prompt") String prompt, @RequestParam("generationType") String generationType) {
        try {
            String decodedPrompt = URLDecoder.decode(prompt, StandardCharsets.UTF_8.name());
            List<byte[]> images = pptService.convertPptToImages(decodedPrompt, generationType);
            List<String> base64Images = new ArrayList<>();
            for (byte[] image : images) {
                base64Images.add(Base64.getEncoder().encodeToString(image));
            }
            return base64Images;
        } catch (Exception e) {
            e.printStackTrace();
            // Log the error for debugging purposes
            log.error("Error occurred while generating PPT preview: ", e);
            // Return an empty list to avoid breaking the frontend
            return Collections.emptyList();
        }
    }


    @PostMapping("/generate")
    public void generatePpt(@RequestParam("prompt") String prompt, @RequestParam("generationType") String generationType, HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        response.setHeader("Content-Disposition", "attachment; filename=handbook.pptx");
        try {
            String decodedPrompt = URLDecoder.decode(prompt, StandardCharsets.UTF_8.name());
            pptService.generatePpt(decodedPrompt, generationType, response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/generatePdf")
    public void generatePdf(@RequestParam("prompt") String prompt, @RequestParam("generationType") String generationType, HttpServletResponse response) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=handbook.pdf");
        try {
            String decodedPrompt = URLDecoder.decode(prompt, StandardCharsets.UTF_8.name());
            pptService.generatePdf(decodedPrompt, generationType, response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}