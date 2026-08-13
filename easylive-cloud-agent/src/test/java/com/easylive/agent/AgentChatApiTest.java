package com.easylive.agent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Agent 接口测试类。
 * 先启动 easylive-cloud-agent，再运行 main 方法。
 */
public class AgentChatApiTest {

    public static void main(String[] args) throws Exception {
        // 1. 接口地址
        URL url = new URL("http://127.0.0.1:7074/agent/chat");

        // 2. 要发送的问题
        String message = "帮我找一些nasa相关的视频";
        String json = "{\"message\":\"" + message + "\"}";

        // 3. 创建 POST 请求
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        connection.setDoOutput(true);

        // 4. 发送请求
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
        }

        // 5. 打印结果
        int statusCode = connection.getResponseCode();
        InputStream inputStream = statusCode >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();

        System.out.println("HTTP状态码：" + statusCode);
        System.out.println("接口返回：");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        connection.disconnect();
    }
}
