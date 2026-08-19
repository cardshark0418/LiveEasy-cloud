package com.easylive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 关键词与向量混合检索配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "hybrid-search")
public class HybridSearchProperties {

    /** 每一路最多召回的结果数。 */
    private Integer recallSize = 10;

    /** RRF 的常数。 */
    private Double rrfK = 60D;

    /** ES BM25 关键词相关性最低分数。 */
    private Float keywordMinScore = 0.1F;

    /** 向量检索的最低余弦相似度，脚本分数会在查询时加 1。 */
    private Float vectorMinCosineSimilarity = 0.35F;
}
