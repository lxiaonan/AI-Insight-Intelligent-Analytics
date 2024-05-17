package com.xiaonan.xnbi.utils.text;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 1、通用文字识别
 * 2、支持中英文,支持手写和印刷文字。
 * 3、在倾斜文字上效果有提升，同时支持部分生僻字的识别。
 * 4、图片格式支持jpg格式、jpeg格式、png格式、bmp格式，且需保证图像文件大小base64编码后不超过10MB
 */
public class UniversalCharacterRecognition {
    private String requestUrl = "https://api.xf-yun.com/v1/private/sf8e6aca1";

    //控制台获取以下信息
    private static String APPID = "*";
    private static String apiSecret = "*";
    private static String apiKey = "*";

    //文件存放位置
    private static File file = null;

    //解析json
    private static Gson gson = new Gson();

    public String getRes(File file) {
        this.file = file;
        UniversalCharacterRecognition demo = new UniversalCharacterRecognition();
        String res = "";
        try {
            String resp = demo.doRequest();
            System.out.println("resp=>" + resp);
            JsonParse myJsonParse = gson.fromJson(resp, JsonParse.class);
            String textBase64Decode = new String(Base64.getDecoder().decode(myJsonParse.payload.result.text), "UTF-8");
            JSONObject jsonObject = JSON.parseObject(textBase64Decode);
            res = jsonObject.getJSONArray("pages").getJSONObject(0).getJSONArray("lines").getJSONObject(0).getJSONArray("words").getJSONObject(0).getString("content");
            System.out.println("text字段Base64解码后=>" + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 请求主方法
     *
     * @return 返回服务结果
     * @throws Exception 异常
     */
    public String doRequest() throws Exception {
        URL realUrl = new URL(buildRequetUrl());
        URLConnection connection = realUrl.openConnection();
        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-type", "application/json");
        OutputStream out = httpURLConnection.getOutputStream();
        String params = buildParam();
        System.out.println("params=>" + params);
        out.write(params.getBytes());
        out.flush();
        InputStream is = null;
        try {
            is = httpURLConnection.getInputStream();
        } catch (Exception e) {
            is = httpURLConnection.getErrorStream();
            throw new Exception("make request error:" + "code is " + httpURLConnection.getResponseMessage() + readAllBytes(is));
        }
        return readAllBytes(is);
    }

    /**
     * 处理请求URL
     * 封装鉴权参数等
     *
     * @return 处理后的URL
     */
    public String buildRequetUrl() {
        URL url = null;
        // 替换调schema前缀 ，原因是URL库不支持解析包含ws,wss schema的url
        String httpRequestUrl = requestUrl.replace("ws://", "http://").replace("wss://", "https://");
        try {
            url = new URL(httpRequestUrl);
            //获取当前日期并格式化
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());
            String host = url.getHost();
            /*if (url.getPort()!=80 && url.getPort() !=443){
                host = host +":"+String.valueOf(url.getPort());
            }*/
            StringBuilder builder = new StringBuilder("host: ").append(host).append("\n").//
                    append("date: ").append(date).append("\n").//
                    append("POST ").append(url.getPath()).append(" HTTP/1.1");
            //System.err.println(builder.toString());
            Charset charset = Charset.forName("UTF-8");
            Mac mac = Mac.getInstance("hmacsha256");
            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
            String sha = Base64.getEncoder().encodeToString(hexDigits);
            String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
            String authBase = Base64.getEncoder().encodeToString(authorization.getBytes(charset));
            return String.format("%s?authorization=%s&host=%s&date=%s", requestUrl, URLEncoder.encode(authBase), URLEncoder.encode(host), URLEncoder.encode(date));
        } catch (Exception e) {
            throw new RuntimeException("assemble requestUrl error:" + e.getMessage());
        }
    }

    /**
     * 组装请求参数
     * 直接使用示例参数，
     * 替换部分值
     *
     * @return 参数字符串
     */
    private String buildParam() throws IOException {
        String param = "{" +
                "    \"header\": {" +
                "        \"app_id\": \"" + APPID + "\"," +
                "        \"status\": 3" +
                "    }," +
                "    \"parameter\": {" +
                "        \"sf8e6aca1\": {" +
                "            \"category\": \"ch_en_public_cloud\"," +
                "            \"result\": {" +
                "                \"encoding\": \"utf8\"," +
                "                \"compress\": \"raw\"," +
                "                \"format\": \"json\"" +
                "            }" +
                "        }" +
                "    }," +
                "    \"payload\": {" +
                "        \"sf8e6aca1_data_1\": {" +
                "            \"encoding\": \"jpg\"," +
                "            \"status\": " + 3 + "," +
                "            \"image\": \"" + Base64.getEncoder().encodeToString(read(file)) + "\"" +
                "        }" +
                "    }" +
                "}";
        return param;
    }

    /**
     * 读取流数据
     *
     * @param is 流
     * @return 字符串
     * @throws IOException 异常
     */
    private String readAllBytes(InputStream is) throws IOException {
        byte[] b = new byte[1024];
        StringBuilder sb = new StringBuilder();
        int len = 0;
        while ((len = is.read(b)) != -1) {
            sb.append(new String(b, 0, len, "utf-8"));
        }
        return sb.toString();
    }

    public static byte[] read(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        byte[] data = inputStream2ByteArray(in);
        in.close();
        return data;
    }

    private static byte[] inputStream2ByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }
}

//解析json
class JsonParse {
    Header header;
    Payload payload;
}

class Header {
    int code;
    String message;
    String sid;
}

class Payload {
    Result result;
}

class Result {
    String text;
}
