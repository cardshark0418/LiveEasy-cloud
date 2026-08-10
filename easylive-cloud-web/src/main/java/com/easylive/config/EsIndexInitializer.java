package com.easylive.config;

import com.easylive.component.EsSearchComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Web 服务启动后初始化 Elasticsearch 索引。
 */
@Component
@Order(20)
@Slf4j
public class EsIndexInitializer implements ApplicationRunner {

    private static final int MAX_RETRIES = 15;
    private static final long RETRY_DELAY_MILLIS = 2000L;

    @Resource
    private EsSearchComponent esSearchComponent;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                esSearchComponent.createIndex();
                log.info("Elasticsearch 视频索引初始化完成。");
                return;
            } catch (Exception e) {
                if (attempt == MAX_RETRIES) {
                    log.error("Elasticsearch 视频索引初始化失败，已重试 {} 次。", MAX_RETRIES, e);
                    throw e;
                }
                log.warn("Elasticsearch 尚未就绪，准备重试索引初始化（第 {}/{} 次）。", attempt, MAX_RETRIES);
                Thread.sleep(RETRY_DELAY_MILLIS);
            }
        }
    }
}
