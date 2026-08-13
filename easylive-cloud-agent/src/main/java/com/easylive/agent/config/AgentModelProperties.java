package com.easylive.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "agent.model")
public class AgentModelProperties {

    private String apiKey;
    private String baseUrl;
    private String modelName;
    private Double temperature = 0.3D;
}
