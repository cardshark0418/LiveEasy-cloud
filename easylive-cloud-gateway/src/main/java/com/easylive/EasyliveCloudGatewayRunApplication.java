package com.easylive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        // 强制排除 Seata 的自动配置
        io.seata.spring.boot.autoconfigure.SeataAutoConfiguration.class,
        io.seata.spring.boot.autoconfigure.SeataDataSourceAutoConfiguration.class
})
public class EasyliveCloudGatewayRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyliveCloudGatewayRunApplication.class, args);
    }
}