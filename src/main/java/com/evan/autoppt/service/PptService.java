package com.evan.autoppt.service;

import com.evan.autoppt.utils.PptTemplate;
import com.evan.autoppt.utils.SlideContent;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PptService {

    private ByteArrayOutputStream pptContent;


    public List<byte[]> convertPptToImages(String prompt, String generationType) throws Exception {

            generatePptContent(prompt, generationType, null);


        try (ByteArrayInputStream bais = new ByteArrayInputStream(pptContent.toByteArray());
             XMLSlideShow ppt = new XMLSlideShow(bais)) {
            List<byte[]> images = new ArrayList<>();
            for (XSLFSlide slide : ppt.getSlides()) {
                BufferedImage img = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = img.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                slide.draw(graphics);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                images.add(baos.toByteArray());
            }
            return images;
        }
    }

    public void generatePpt(String prompt, String generationType, OutputStream outputStream) throws Exception {

        generatePptContent(prompt, generationType, null);

        pptContent.writeTo(outputStream);
    }

    public void generatePdf(String prompt, String generationType, OutputStream outputStream) throws Exception {
        if (pptContent == null) {
            generatePptContent(prompt, generationType, null);
        }
        ByteArrayOutputStream pdfContent = new ByteArrayOutputStream();
        AutoPptGenerator.convertPptToPdf(pptContent, pdfContent);
        pdfContent.writeTo(outputStream);
    }

    private void generatePptContent(String prompt, String generationType, AtomicInteger progress) throws Exception {
        String markdownContent = AutoPptGenerator.callDeepSeekApi(prompt, generationType);
        List<SlideContent> slides = AutoPptGenerator.parseMarkdown(markdownContent);

        PptTemplate template = new PptTemplate(
                new Color(0, 255, 255),    // Title color
                new Color(255, 255, 255),  // Body color
                36.0,                      // Title font size
                20.0,                      // Body font size
                "Microsoft Sans Serif",    // Title font
                "Microsoft Sans Serif",    // Body font
                "pic/1.png"                // Background image path
        );

        pptContent = new ByteArrayOutputStream();
        AutoPptGenerator.generatePptFile(slides, pptContent, template);

        if (progress != null) {
            progress.set(100);
        }
    }
}