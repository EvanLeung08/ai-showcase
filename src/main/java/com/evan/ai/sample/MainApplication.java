/*
package com.evan.autoppt.sample;


import com.day.util.MyUtil;
import com.google.gson.Gson;
import okhttp3.HttpUrl;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

// 主函数入口
public class MainApplication {

    public static final String hostUrl = "https://spark-api.cn-huabei-1.xf-yun.com/v2.1/tti";
    public static final String appid = "******"; //这里填写APPID
    public static final String apiSecret = "\"******"; //这里填写Secret
    public static final String apiKey = "\"******"; //这里填写Key
    public static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
        // URL地址正确
        //System.err.println(authUrl);
        String json = "{\n" + "  \"header\": {\n" + "    \"app_id\": \"" + appid + "\",\n" + "    \"uid\": \"" + UUID.randomUUID().toString().substring(0, 15) + "\"\n" + "  },\n" + "  \"parameter\": {\n" + "    \"chat\": {\n" + "      \"domain\": \"s291394db\",\n" + "      \"temperature\": 0.5,\n" + "      \"max_tokens\": 4096,\n" + "      \"width\": 1024,\n" + "      \"height\": 1024\n" + "    }\n" + "  },\n" + "  \"payload\": {\n" + "    \"message\": {\n" + "      \"text\": [\n" + "        {\n" + "          \"role\": \"user\",\n" + "          \"content\": \"帮我画一个小鸟\"\n" + "        }\n" + "      ]\n" + "    }\n" + "  }\n" + "}";
        // 发起Post请求
        //System.err.println(json);
        String res = MyUtil.doPostJson(authUrl, null, json);
        System.out.println(res);
        JsonParse jsonParse = gson.fromJson(res, JsonParse.class);
        byte[] imageBytes = Base64.getDecoder().decode(jsonParse.payload.choices.text.get(0).content);
        // 指定输出图片的文件路径
        String outputPath = "src/main/resources/1.png";
        try (FileOutputStream imageOutFile = new FileOutputStream(outputPath)) {
            // 将字节数组写入文件
            imageOutFile.write(imageBytes);
            System.out.println("图片已成功保存到: " + outputPath);
        } catch (IOException e) {
            System.err.println("保存图片时出错: " + e.getMessage());
        }
    }

    // 鉴权方法
    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        // 时间
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        // date="Thu, 12 Oct 2023 03:05:28 GMT";
        // 拼接
        String preStr = "host: " + url.getHost() + "\n" + "date: " + date + "\n" + "POST " + url.getPath() + " HTTP/1.1";
        // System.err.println(preStr);
        // SHA256加密
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);

        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        // Base64加密
        String sha = Base64.getEncoder().encodeToString(hexDigits);
        // System.err.println(sha);
        // 拼接
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        // 拼接地址
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder().//
                addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8))).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();

        // System.err.println(httpUrl.toString());
        return httpUrl.toString();
    }
}

class JsonParse {
    Payload payload;
}

class Payload {
    Choices choices;
}

class Choices {
    List<Text> text;
}

class Text {
    String content;
}*/
