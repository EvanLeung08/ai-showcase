package com.evan.ai.service;

import com.evan.ai.provider.AiApiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TranslationService {
    @Qualifier("xunfeiProvider")
    @Autowired
    private AiApiProvider aiApiProvider;

    public String translate(String text, String sourceLang, String targetLang) throws Exception {
        log.info("▄▄▄▄▄ [翻译开始] 方向: {}→{} 字符数: {}", sourceLang, targetLang, text.length());

        try {
            String translated = aiApiProvider.translateText(text, sourceLang, targetLang);
            log.info("▀▀▀▀▀ [翻译完成] 原文: {}... → 译文: {}...",
                    text.substring(0, Math.min(20, text.length())),
                    translated.substring(0, Math.min(20, translated.length())));
            return translated;
        } catch (Exception e) {
            log.error("××××× [翻译失败] 错误: {}", e.getMessage());
            throw e;
        }
    }
}