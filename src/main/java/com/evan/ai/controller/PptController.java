package com.evan.ai.controller;

import com.evan.ai.service.AutoPptGenerator;
import com.evan.ai.service.ImageGenerationService;
import com.evan.ai.service.PptService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class PptController {

    @Autowired
    private PptService pptService;

    private static final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
    @Autowired  // 添加这个自动注入
    private AutoPptGenerator autoPptGenerator;
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
    public List<String> preview(@RequestParam("prompt") String prompt, @RequestParam("generationType") String generationType) {
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

    // 在PptController类中添加
    @PostMapping("/getMnemonicWithImages")
    @ResponseBody
    public List<MnemonicWithImageDTO> getMnemonicWithImages(
            @RequestParam("prompt") String prompt) throws Exception {

        String decodedPrompt = URLDecoder.decode(prompt, StandardCharsets.UTF_8.name());
        String markdownContent = autoPptGenerator.callDeepSeekApi(decodedPrompt, "mnemonics");

        return Arrays.stream(decodedPrompt.split(","))
                .map(String::trim)
                .map(word -> {
                    String mnemonic = extractMnemonicContent(markdownContent, word);
                    List<String> images = generateMnemonicImages(mnemonic);
                    return new MnemonicWithImageDTO(word, mnemonic, images);
                }).collect(Collectors.toList());
    }

    private String extractMnemonicContent(String markdown, String word) {
        Pattern pattern = Pattern.compile("# " + word + "([\\s\\S]*?)(?=#|$)");
        Matcher matcher = pattern.matcher(markdown);
        String rawContent = matcher.find() ? matcher.group(1).trim() : "未找到助记信息";

        // 新增Markdown转换逻辑
        Parser parser = Parser.builder().build();
        Node document = parser.parse(rawContent);
        return htmlRenderer.render(document);
    }

    private List<String> generateMnemonicImages(String mnemonic) {
        // 从助记文本中提取关键词生成图片（修改为只保留第一张有效图片）
        List<String> prompts = extractImagePrompts(mnemonic);
        return prompts.stream()
                .findFirst() // 只取第一个联想记忆提示词
                .map(prompt -> {
                    try {
                        return imageGenerationService.generateImage(prompt);
                    } catch (Exception e) {
                        log.error("图片生成失败", e);
                        return "";
                    }
                })
                .filter(StringUtils::hasText)
                .map(Collections::singletonList) // 包装成单元素列表
                .orElse(Collections.emptyList());
    }

    private List<String> extractImagePrompts(String mnemonic) {
        // 使用正则提取联想词，示例匹配 "联想记忆：[内容]"
        Pattern pattern = Pattern.compile("联想记忆：([^\\n]+)");
        Matcher matcher = pattern.matcher(mnemonic);
        return matcher.find() ?
                Arrays.asList(matcher.group(1).split("，")) :
                Collections.emptyList();
    }

    // 添加DTO类
    @Data
    @AllArgsConstructor
    static class MnemonicWithImageDTO {
        private String word;
        private String mnemonic;
        private List<String> images;
    }

}