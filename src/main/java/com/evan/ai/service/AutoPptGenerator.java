package com.evan.ai.service;

import com.evan.ai.provider.AiApiProvider;
import com.evan.ai.utils.PptTemplate;
import com.evan.ai.utils.SlideContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AutoPptGenerator {

    @Qualifier("xunfeiProvider")
    @Autowired
    private AiApiProvider aiApiProvider;

    public String callDeepSeekApi(String words, String generationType) throws Exception {
        return aiApiProvider.generateWithTemplate(words, generationType);
    }



    public List<SlideContent> parseMarkdown(String markdown) {
        List<SlideContent> slides = new ArrayList<>();
        Parser parser = Parser.builder().build();
        for (String slideMarkdown : markdown.split("---")) {
            Node document = parser.parse(slideMarkdown.trim());
            SlideContent slideContent = new SlideContent();
            document.accept(new AbstractVisitor() {
                @Override
                public void visit(Heading heading) {
                    if (heading.getLevel() == 1) {
                        slideContent.setTitle(extractText(heading));
                    } else {
                        slideContent.addBody(extractText(heading));
                    }
                }

                @Override
                public void visit(Paragraph paragraph) {
                    slideContent.addBody(extractText(paragraph));
                }

                @Override
                public void visit(StrongEmphasis strongEmphasis) {
                    slideContent.addBody(extractText(strongEmphasis));
                }

                @Override
                public void visit(BulletList bulletList) {
                    StringBuilder listContent = new StringBuilder();
                    bulletList.accept(new AbstractVisitor() {
                        @Override
                        public void visit(ListItem listItem) {
                            listContent.append("- ").append(extractText(listItem)).append("\n");
                        }
                    });
                    slideContent.addBody(listContent.toString());
                }
            });
            if (slideContent.getTitle() == null) {
                slideContent.setTitle("Untitled Slide");
            }
            slides.add(slideContent);
        }
        return slides;
    }

    private static String extractText(Node node) {
        StringBuilder textContent = new StringBuilder();
        node.accept(new AbstractVisitor() {
            @Override
            public void visit(org.commonmark.node.Text text) {
                textContent.append(text.getLiteral());
            }

            @Override
            public void visit(SoftLineBreak softLineBreak) {
                textContent.append("\n");
            }

            @Override
            public void visit(HardLineBreak hardLineBreak) {
                textContent.append("\n");
            }
        });
        return textContent.toString();
    }

    public static void generatePptFile(List<SlideContent> slides, OutputStream outputStream, PptTemplate template) throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.setPageSize(new java.awt.Dimension(1280, 720));

            for (SlideContent content : slides) {
                XSLFSlide slide = ppt.createSlide();
                template.applyTemplate(ppt, slide, content.getTitle(), String.join("\n", content.getBody()));

                if (content.getTitle().contains("图表")) {
                    BufferedImage chartImage = createChart(content.getBody());
                    addChartToSlide(ppt, slide, chartImage);
                }
            }

            ppt.write(outputStream);
        }
    }

    private static BufferedImage createChart(List<String> chartData) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (String data : chartData) {
            String[] parts = data.split(":");
            if (parts.length == 2) {
                String label = parts[0].trim();
                double value = Double.parseDouble(parts[1].trim());
                dataset.addValue(value, label, "");
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Chart Title",
                "Category",
                "Value",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        return chart.createBufferedImage(640, 480);
    }

    private static void addChartToSlide(XMLSlideShow ppt, XSLFSlide slide, BufferedImage chartImage) throws IOException {
        byte[] imageBytes;
        try (var baos = new java.io.ByteArrayOutputStream()) {
            ImageIO.write(chartImage, "png", baos);
            imageBytes = baos.toByteArray();
        }

        PictureData idx = ppt.addPicture(imageBytes, PictureData.PictureType.PNG);
        XSLFPictureShape pic = slide.createPicture(idx);
        pic.setAnchor(new java.awt.geom.Rectangle2D.Double(100, 150, 640, 480));
    }

    public static void convertPptToPdf(OutputStream pptOutputStream, OutputStream pdfOutputStream) throws IOException {
        try (PDDocument pdfDocument = new PDDocument();
             XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(((ByteArrayOutputStream) pptOutputStream).toByteArray()))) {

            for (XSLFSlide slide : ppt.getSlides()) {
                // Use a higher resolution for the BufferedImage
                int width = 1920;
                int height = 1080;
                BufferedImage slideImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = slideImage.createGraphics();

                // Set rendering hints for better quality
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                slide.draw(graphics);
                graphics.dispose();

                PDPage pdfPage = new PDPage(new PDRectangle(width, height));
                pdfDocument.addPage(pdfPage);

                PDImageXObject pdImage = PDImageXObject.createFromByteArray(pdfDocument, convertBufferedImageToByteArray(slideImage), "slide");
                try (PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, pdfPage)) {
                    contentStream.drawImage(pdImage, 0, 0, width, height);
                }
            }

            pdfDocument.save(pdfOutputStream);
        }
    }

    private static byte[] convertBufferedImageToByteArray(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }
    }
}