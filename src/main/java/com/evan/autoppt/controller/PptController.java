package com.evan.autoppt.controller;

import com.evan.autoppt.service.PptService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Controller
public class PptController {

    @Autowired
    private PptService pptService;


    @PostMapping("/preview")
    @ResponseBody
    public List<String> previewPpt(@RequestParam("prompt") String prompt, @RequestParam("generationType") String generationType) {
        try {
            String decodedPrompt = URLDecoder.decode(prompt, StandardCharsets.UTF_8.name());
            List<byte[]> images = pptService.convertPptToImages(decodedPrompt,generationType);
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