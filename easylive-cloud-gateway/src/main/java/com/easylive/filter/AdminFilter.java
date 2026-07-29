package com.easylive.filter;

import com.easylive.entity.constants.Constants;
import com.easylive.enums.ResponseCodeEnum;
import com.easylive.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Component
@Slf4j
public class AdminFilter extends AbstractGatewayFilterFactory {
    private final static String URL_ACCOUNT = "/account";
    private final static String URL_FILE = "/file";

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (request.getURI().getRawPath().contains(URL_ACCOUNT)) {
                return chain.filter(exchange);
            }
            String token = getTokenFromCookie(request);


            if (StringUtils.isEmpty(token)) {
                throw new BusinessException(ResponseCodeEnum.CODE_901);
            }
            String key = Constants.REDIS_KEY_ADMIN_TOKEN + token;
            Object o = redisTemplate.opsForValue().get(key);
            if(o==null){
                throw new BusinessException(ResponseCodeEnum.CODE_901);
            }
            return chain.filter(exchange);
        };
    }

    private String getTokenFromCookie(ServerHttpRequest request) {
        if (request.getCookies() == null || request.getCookies().getFirst("adminToken") == null) {
            return null;
        }
        return request.getCookies().getFirst("adminToken").getValue();
    }

}
