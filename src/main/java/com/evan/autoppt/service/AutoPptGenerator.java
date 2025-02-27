package com.evan.autoppt.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import com.evan.autoppt.utils.PptTemplate;
import com.evan.autoppt.utils.SlideContent;
import lombok.extern.slf4j.Slf4j;
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
import org.json.JSONException;
import org.json.JSONObject;

@Slf4j
public class AutoPptGenerator {


    private static final String API_ENDPOINT = "https://spark-api-open.xf-yun.com/v1/chat/completions";

    public static String callDeepSeekApi(String prompt) throws Exception {
        StringBuilder aggregatedContent = new StringBuilder();
        int part = 1;
        boolean hasMoreContent = true;

        while (hasMoreContent) {
            String requestBody = String.format("""
        {
            "model": "4.0Ultra",
            "messages": [
            {"role": "system", "content": "你是一个单词记忆专家和世界记忆大师，你的任务是帮助用户记忆英语单词。"},
                {"role": "user", "content": "%s\\n请用记忆大师的方式，先使用输入的全部单词创作一篇中英对照的英语故事，方便记忆，然后对输入的每个英语单词提供 1.记忆大师助记(即词根词缀+以熟记生等方法自由组合结合联想记忆)、2.中文解释、3.音标、4.例句、5.涉及的前缀后缀和词根、6.相同词根词缀的单词、7.反义词、8.同义词、9.近义词、10.常见词组搭配等。\\n请用Markdown格式输出PPT内容，尽可能详细，用'---'分隔每页幻灯片。请提供第%d部分。"}
            ],
            "stream": false,
            "temperature": 0.7
        }
        """, prompt.replace("\"", "\\\""),part);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(120))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + System.getProperty("API_KEY"))
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            int maxRetries = 3;
            int attempt = 0;
            while (attempt < maxRetries) {
                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    String responseBody = response.body();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String content = jsonResponse.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        log.info("Response: " + content);
                        aggregatedContent.append(content.replace("\\n", "\n"));
                        hasMoreContent = content.contains("请提供第" + (part + 1) + "部分");
                        part++;
                        break;
                    } catch (JSONException e) {
                        return responseBody;
                    }
                } catch (HttpTimeoutException e) {
                    attempt++;
                    if (attempt >= maxRetries) {
                        throw new Exception("Request timed out after " + maxRetries + " attempts", e);
                    }
                }
            }
        }
        return aggregatedContent.toString();
    }

    public static List<SlideContent> parseMarkdown(String markdown) {
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
            public void visit(Text text) {
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
}