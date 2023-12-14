package com.xiaonan.xnbi.utils.image;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import okhttp3.*;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class BigModelImage extends WebSocketListener {
    // 地址与鉴权信息  https://spark-api.xf-yun.com/v1.1/chat   1.5地址  domain参数为general
    // 地址与鉴权信息  https://spark-api.xf-yun.com/v2.1/chat   2.0地址  domain参数为generalv2
    public static final String hostUrl = "https://spark-api.cn-huabei-1.xf-yun.com/v2.1/image";
    public static final String appid = "b9706276";
    public static final String apiSecret = "MjliODA2YzdiYWUwZTAxMjYzZDI5NGJh";
    public static final String apiKey = "3669b2806547033bab68989b079ed2f2";

//    public static List<RoleContent> historyList = new ArrayList<>(); // 对话历史存储集合

    public static String totalAnswer = ""; // 大模型的答案汇总

    // 环境治理的重要性  环保  人口老龄化  我爱我的祖国
    public static String NewQuestion = "";
    public static Boolean ImageAddFlag = false; // 判断是否添加了图片信息

    public static Long imageId;

    public static final Gson gson = new Gson();
    private static RedissonClient redissonClient;
    //外部传入文件
    public static File file;
    //外部传入问题

    // 个性化参数
    private String userId;
    private Boolean wsCloseFlag;

    private static Boolean totalFlag = true; // 控制提示用户是否输入

    // 构造函数
    public BigModelImage(String userId, Boolean wsCloseFlag) {
        this.userId = userId;
        this.wsCloseFlag = wsCloseFlag;
    }

    public BigModelImage(File file, RedissonClient redissonClient,Long id) {
        this.file = file;
        this.redissonClient = redissonClient;
        this.imageId = id;
    }
    public void getResult(String question){
        try {
            // 个性化参数入口，如果是并发使用，可以在这里模拟
            NewQuestion = question;
            // 构建鉴权url
            String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
            OkHttpClient client = new OkHttpClient.Builder().build();
            String url = authUrl.toString().replace("http://", "ws://").replace("https://", "wss://");
            Request request = new Request.Builder().url(url).build();
            for (int i = 0; i < 1; i++) {
                totalAnswer = "";
                WebSocket webSocket = client.newWebSocket(request, new BigModelImage(i + "",
                        false));
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

//    public static boolean canAddHistory() {  // 由于历史记录最大上线1.2W左右，需要判断是能能加入历史
//        int history_length = 0;
//        for (RoleContent temp : historyList) {
//            history_length = history_length + temp.content.length();
//        }
//        // System.out.println(history_length);
//        if (history_length > 1200000000) { // 这里限制了总上下文携带，图片理解注意放大 ！！！
//            historyList.remove(0);
//            return false;
//        } else {
//            return true;
//        }
//    }

    // 线程来发送音频与参数
    class MyThread extends Thread {
        private WebSocket webSocket;

        public MyThread(WebSocket webSocket) {
            this.webSocket = webSocket;
        }

        public void run() {
            try {
                JSONObject requestJson = new JSONObject();

                JSONObject header = new JSONObject();  // header参数
                header.put("app_id", appid);
                header.put("uid", UUID.randomUUID().toString().substring(0, 10));

                JSONObject parameter = new JSONObject(); // parameter参数
                JSONObject chat = new JSONObject();
                chat.put("domain", "image");
                chat.put("temperature", 0.5);
                chat.put("max_tokens", 4096);
                chat.put("auditing", "default");
                parameter.put("chat", chat);

                JSONObject payload = new JSONObject(); // payload参数
                JSONObject message = new JSONObject();
                JSONArray text = new JSONArray();

                // 历史问题获取
//                if (historyList.size() > 0) { // 保证首个添加的是图片
//                    for (RoleContent tempRoleContent : historyList) {
//                        if (tempRoleContent.content_type.equals("image")) { // 保证首个添加的是图片
//                            text.add(JSON.toJSON(tempRoleContent));
//                            ImageAddFlag = true;
//                        }
//                    }
//                }
//                if (historyList.size() > 0) {
//                    for (RoleContent tempRoleContent : historyList) {
//                        if (!tempRoleContent.content_type.equals("image")) { // 添加费图片类型
//                            text.add(JSON.toJSON(tempRoleContent));
//                        }
//                    }
//                }

                // 最新问题
                RoleContent roleContent = new RoleContent();
                // 添加图片信息
                if (!ImageAddFlag) {
                    roleContent.role = "user";
                    //读入外部文件
                    roleContent.content = Base64.getEncoder().encodeToString(ImageUtil.readByFile(file));
                    roleContent.content_type = "image";
                    text.add(JSON.toJSON(roleContent));
//                    historyList.add(roleContent);
                }
                // 添加对图片提出要求的信息
                RoleContent roleContent1 = new RoleContent();
                roleContent1.role = "user";
                roleContent1.content = NewQuestion;
                roleContent1.content_type = "text";
                text.add(JSON.toJSON(roleContent1));
//                historyList.add(roleContent1);


                message.put("text", text);
                payload.put("message", message);


                requestJson.put("header", header);
                requestJson.put("parameter", parameter);
                requestJson.put("payload", payload);
                // System.err.println(requestJson); // 可以打印看每次的传参明细
                webSocket.send(requestJson.toString());
                // 等待服务端返回完毕后关闭
                while (true) {
                    // System.err.println(wsCloseFlag + "---");
                    Thread.sleep(200);
                    if (wsCloseFlag) {
                        break;
                    }
                }
                //ai数据生成完毕哦，向redis写入键
                RBucket<String> bucket = redissonClient.getBucket("image" + imageId);
                bucket.set("true");
                webSocket.close(1000, "");
            } catch (Exception e) {
                //失败
                RBucket<String> bucket = redissonClient.getBucket("complete" + imageId);
                bucket.set("false");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        System.out.print("大模型：");
        //开启线程执行操作
        MyThread myThread = new MyThread(webSocket);
        myThread.start();
    }

    Thread sleepThread;

    public String getReturn() {
        // 创建一个新的线程
        sleepThread = new Thread(() -> {
            try {
//                // 线程休眠7秒
//                Thread.sleep(40000);
                //循环等待ai生成完毕
                int times = 0;//现在循环次数
                while (true) {
                    if (times > 90)
                        break;
                    Thread.sleep(2000);
                    RBucket<String> bucket = redissonClient.getBucket("image" + imageId);
                    String res = bucket.get();
                    if (Objects.equals(res, "true")) {
                        bucket.delete();//取完就删除
                        break;
                    }
                    times++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.err.println("Sleep thread finished");
        });

        // 启动线程
        sleepThread.start();

        try {
            // 等待线程的结束
            sleepThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return totalAnswer;
    }
    @Override
    public void onMessage(WebSocket webSocket, String text) {
        // System.out.println(userId + "用来区分那个用户的结果" + text);
        JsonParse myJsonParse = gson.fromJson(text, JsonParse.class);
        if (myJsonParse.header.code != 0) {
            System.out.println("发生错误，错误码为：" + myJsonParse.header.code);
            System.out.println("本次请求的sid为：" + myJsonParse.header.sid);
            webSocket.close(1000, "");
        }
        List<Text> textList = myJsonParse.payload.choices.text;
        for (Text temp : textList) {
//            System.out.print(temp.content);
            totalAnswer = totalAnswer + temp.content;
        }
        if (myJsonParse.header.status == 2) {
            // 可以关闭连接，释放资源
            System.out.println();
            System.out.println("*************************************************************************************");
//            if (canAddHistory()) {
//                RoleContent roleContent = new RoleContent();
//                roleContent.setRole("assistant");
//                roleContent.setContent(totalAnswer);
//                roleContent.setContent_type("text");
//                historyList.add(roleContent);
//            } else {
//                historyList.remove(0);
//                RoleContent roleContent = new RoleContent();
//                roleContent.setRole("assistant");
//                roleContent.setContent(totalAnswer);
//                roleContent.setContent_type("text");
//                historyList.add(roleContent);
//            }
            wsCloseFlag = true;
            totalFlag = true;
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        try {
            if (null != response) {
                int code = response.code();
                System.out.println("onFailure code:" + code);
                System.out.println("onFailure body:" + response.body().string());
                if (101 != code) {
                    System.out.println("connection failed");
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    // 鉴权方法
    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        // 时间
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        // 拼接
        String preStr = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";
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

    //返回的json结果拆解
    class JsonParse {
        Header header;
        Payload payload;
    }

    class Header {
        int code;
        int status;
        String sid;
    }

    class Payload {
        Choices choices;
    }

    class Choices {
        List<Text> text;
    }

    class Text {
        String role;
        String content;
    }

    class RoleContent {
        String role;
        String content;

        String content_type;

        public String getContent_type() {
            return content_type;
        }

        public void setContent_type(String content_type) {
            this.content_type = content_type;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
