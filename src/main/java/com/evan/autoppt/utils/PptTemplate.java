package com.evan.autoppt.utils;

import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;

public class PptTemplate {
    private final Color titleColor;
    private final Color bodyColor;
    private final double titleFontSize;
    private final double bodyFontSize;
    private final String titleFont;
    private final String bodyFont;
    private final String backgroundImagePath;

    public PptTemplate(Color titleColor, Color bodyColor, double titleFontSize, double bodyFontSize, String titleFont, String bodyFont, String backgroundImagePath) {
        this.titleColor = titleColor;
        this.bodyColor = bodyColor;
        this.titleFontSize = titleFontSize;
        this.bodyFontSize = bodyFontSize;
        this.titleFont = titleFont;
        this.bodyFont = bodyFont;
        this.backgroundImagePath = backgroundImagePath;
    }

    public void applyTemplate(XMLSlideShow ppt, XSLFSlide slide, String titleText, String bodyText) throws IOException {
        // Set background image
        if (backgroundImagePath != null && !backgroundImagePath.isEmpty()) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("static/" + backgroundImagePath)) {
                if (is == null) {
                    throw new IOException("Background image not found: " + backgroundImagePath);
                }
                byte[] pictureData = is.readAllBytes();
                PictureData idx = ppt.addPicture(pictureData, PictureData.PictureType.PNG);
                XSLFPictureShape pic = slide.createPicture(idx);
                pic.setAnchor(new Rectangle2D.Double(0, 0, ppt.getPageSize().getWidth(), ppt.getPageSize().getHeight()));
            }
        }

        // Add title
        if (titleText != null && !titleText.isEmpty()) {
            XSLFTextShape title = slide.createTextBox();
            title.setText(titleText);
            title.setAnchor(new Rectangle2D.Double(50, 30, 1180, 60));
            title.setFillColor(titleColor);
            setTitleFontSizeAndFont(title, titleFontSize, titleFont);
        }


        // Add body
        if (bodyText != null && !bodyText.isEmpty()) {
            XSLFTextShape body = slide.createTextBox();
            body.setText(bodyText);
            body.setAnchor(new Rectangle2D.Double(100, 120, 1080, 500));
            body.setFillColor(bodyColor);
            setBodyFontSizeAndFont(body, bodyFontSize, bodyFont);
        }
    }

    private void setTitleFontSizeAndFont(XSLFTextShape shape, double fontSize, String font) {
        for (var paragraph : shape.getTextParagraphs()) {
            for (var run : paragraph.getTextRuns()) {
                run.setFontSize(fontSize);
                run.setFontFamily(font);
                run.setBold(true); // Set the font to bold
            }
        }
    }
    private void setBodyFontSizeAndFont(XSLFTextShape shape, double fontSize, String font) {
        for (var paragraph : shape.getTextParagraphs()) {
            for (var run : paragraph.getTextRuns()) {
                run.setFontSize(fontSize);
                run.setFontFamily(font);
            }
        }
    }
}