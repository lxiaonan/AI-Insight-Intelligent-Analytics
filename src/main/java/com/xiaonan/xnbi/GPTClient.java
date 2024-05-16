package com.xiaonan.xnbi;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GPTClient {

    public static void main(String[] args) {
        String apiKey = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJsb2dpblR5cGUiOiJsb2dpbiIsImxvZ2luSWQiOjk2MDQwNSwiZGV2aWNlIjoiZGVmYXVsdC1kZXZpY2UiLCJlZmYiOjE3MTgwMDA1MjMyODQsInJuU3RyIjoiYmVRUDZ5ZlRjTFRkNlNBZkd6WUhwUDhvS3pQM056ZUoifQ.HQFcmYPM3g4o04j_QLYZ75bmVDqGrLXIabYdw2jjmOM";

        String prompt = "{\"prompt\":\"c#与Java语言相似吗\",\"history\":[],\"token\":\"\",\"cdkey\":\"\",\"uuid\":\"7938c590153640afb347093067258625\",\"type\":\"3.5\",\"presetId\":1,\"title\":\"假如你是资深PC客户端开发，我现在是一名Java后端开发工程师，如果我想要学习PC客户端开发，你会给我一些什么建议呢，或者说是应该学习那些技术栈和学习方向\",\"id\":\"7938c590153640afb347093067258625\",\"local\":true,\"temp\":0.7}";

        try {
            URL url = new URL("https://chat.aimakex.com/api/chat/chat/sse");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("token",apiKey);
            connection.setDoOutput(true);

            connection.getOutputStream().write(prompt.getBytes());

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                    if (inputLine.contains("anwser")) {
                        inputLine = inputLine.substring(inputLine.indexOf("anwser") + 8);
                    }
                    System.out.println(inputLine);
                }
                in.close();

                // 处理JSON响应数据
                String jsonResponse = response.toString();
                System.out.println("Response: " + jsonResponse);
                // 在这里解析JSON响应并处理多段数据
            } else {
                System.out.println("Error: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
