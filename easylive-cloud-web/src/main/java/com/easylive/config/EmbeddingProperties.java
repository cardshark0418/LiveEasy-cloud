package com.easylive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 视频向量模型配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {

    private boolean enabled = false;
    private String apiKey;
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private String modelName = "text-embedding-v4";
    private Integer dimension = 1024;
    private Long timeoutSeconds = 20L;
}
