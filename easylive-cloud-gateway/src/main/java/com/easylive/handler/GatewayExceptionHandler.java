package com.easylive.handler;


import cn.hutool.json.JSONUtil;
import com.easylive.enums.ResponseCodeEnum;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {
    public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
        log.error("网关请求错误url:{},错误信息", exchange.getRequest().getPath(), throwable);
        ResponseVO responseVO = getResponse(throwable);
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer dataBuffer = response.bufferFactory().wrap(JSONUtil.toJsonStr(responseVO).getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(dataBuffer));
    }

    private ResponseVO getResponse(Throwable throwable) {
        ResponseVO responseVO = new ResponseVO();
        responseVO.setStatus("error");
        if (throwable instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) throwable;
            //404
            if (HttpStatus.NOT_FOUND == responseStatusException.getStatus()) {
                responseVO.setCode(ResponseCodeEnum.CODE_404.getCode());
                responseVO.setInfo(ResponseCodeEnum.CODE_404.getMsg());
                return responseVO;
            } else if (HttpStatus.SERVICE_UNAVAILABLE == responseStatusException.getStatus()) {
                //503
                responseVO.setCode(ResponseCodeEnum.CODE_503.getCode());
                responseVO.setInfo(ResponseCodeEnum.CODE_503.getMsg());
                return responseVO;
            } else {
                responseVO.setCode(responseStatusException.getStatus().value());
                responseVO.setInfo(ResponseCodeEnum.CODE_500.getMsg());
                return responseVO;
            }
            //业务异常
        } else if (throwable instanceof BusinessException) {
            BusinessException exception = (BusinessException) throwable;
            responseVO.setCode(exception.getCode());
            responseVO.setInfo(exception.getMessage());
            return responseVO;
        }
        responseVO.setCode(ResponseCodeEnum.CODE_500.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_500.getMsg());
        return responseVO;
    }
}
