package com.easylive.component;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.easylive.config.EmbeddingProperties;
import com.easylive.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 调用 OpenAI 兼容 Embedding 接口生成文本向量。 */
@Component
@Slf4j
public class EmbeddingClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Resource
    private EmbeddingProperties properties;

    private OkHttpClient httpClient;

    @PostConstruct
    public void init() {
        long timeout = properties.getTimeoutSeconds() == null || properties.getTimeoutSeconds() <= 0
                ? 20L : properties.getTimeoutSeconds();
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    public boolean isEnabled() {
        return properties.isEnabled()
                && properties.getApiKey() != null
                && !properties.getApiKey().trim().isEmpty()
                && properties.getModelName() != null
                && !properties.getModelName().trim().isEmpty();
    }

    public List<Float> embed(String text) {
        if (!isEnabled()) {
            return null;
        }
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException("向量化文本不能为空");
        }

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("model", properties.getModelName());
        requestData.put("input", text);
        if (properties.getDimension() != null && properties.getDimension() > 0) {
            requestData.put("dimensions", properties.getDimension());
        }

        Request request = new Request.Builder()
                .url(buildEmbeddingUrl(properties.getBaseUrl()))
                .addHeader("Authorization", "Bearer " + properties.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(JSON_MEDIA_TYPE, JSONUtil.toJsonStr(requestData)))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new BusinessException("调用向量模型失败，HTTP状态码：" + response.code());
            }

            JSONArray data = JSONUtil.parseObj(responseBody).getJSONArray("data");
            if (data == null || data.isEmpty()) {
                throw new BusinessException("向量模型没有返回向量");
            }
            JSONArray embedding = JSONUtil.parseObj(data.get(0).toString()).getJSONArray("embedding");
            if (embedding == null || embedding.isEmpty()) {
                throw new BusinessException("向量模型返回结果格式不正确");
            }

            List<Float> vector = new ArrayList<>(embedding.size());
            for (Object value : embedding) {
                vector.add(Float.valueOf(String.valueOf(value)));
            }
            if (properties.getDimension() != null && vector.size() != properties.getDimension()) {
                throw new BusinessException("向量维度不一致，配置维度：" + properties.getDimension()
                        + "，实际维度：" + vector.size());
            }
            return vector;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("调用向量模型失败，请检查网络和配置");
        } catch (Exception e) {
            log.error("解析向量模型返回结果失败", e);
            throw new BusinessException("解析向量模型返回结果失败");
        }
    }

    private String buildEmbeddingUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new BusinessException("向量模型接口地址不能为空");
        }
        String url = baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url.endsWith("/embeddings") ? url : url + "/embeddings";
    }
}
