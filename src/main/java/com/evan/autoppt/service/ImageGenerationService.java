package com.evan.autoppt.service;

import com.evan.autoppt.utils.MyUtil;
import com.google.gson.Gson;
import okhttp3.HttpUrl;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ImageGenerationService {
    // 分离host和path定义
    public static final String hostUrl = "https://spark-api.cn-huabei-1.xf-yun.com/v2.1/tti";
    private static final String API_PATH = "/v2.1/tti";
    public static final Gson gson = new Gson();

    public String generateImage(String prompt) throws Exception {

        String authUrl = getAuthUrl(hostUrl, System.getProperty("XF_API_KEY"), System.getProperty("XF_API_SECRET"));
        // URL地址正确
        //System.err.println(authUrl);
        String json = "{\n" + "  \"header\": {\n" + "    \"app_id\": \"" + System.getProperty("XF_APP_ID") + "\",\n" + "    \"uid\": \"" + UUID.randomUUID().toString().substring(0, 15) + "\"\n" + "  },\n" + "  \"parameter\": {\n" + "    \"chat\": {\n" + "      \"domain\": \"s291394db\",\n" + "      \"temperature\": 0.5,\n" + "      \"max_tokens\": 4096,\n" + "      \"width\": 1024,\n" + "      \"height\": 1024\n" + "    }\n" + "  },\n" + "  \"payload\": {\n" + "    \"message\": {\n" + "      \"text\": [\n" + "        {\n" + "          \"role\": \"user\",\n" + "          \"content\": \""+prompt+"\"\n" + "        }\n" + "      ]\n" + "    }\n" + "  }\n" + "}";
        // 发起Post请求
        //System.err.println(json);
        String res = MyUtil.doPostJson(authUrl, null, json);
        System.out.println(res);
        JsonParse jsonParse = gson.fromJson(res, JsonParse.class);
        byte[] imageBytes = Base64.getDecoder().decode(jsonParse.payload.choices.text.get(0).content);


        return Base64.getEncoder().encodeToString(imageBytes);
    }


    // 鉴权方法
    public String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
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
}