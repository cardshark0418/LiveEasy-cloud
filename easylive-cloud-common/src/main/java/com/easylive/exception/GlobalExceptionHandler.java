package com.easylive.exception;

import com.easylive.entity.vo.ResponseVO;
import com.easylive.enums.ResponseCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一处理业务服务中的异常，确保前端能够拿到可读的错误信息。
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.easylive.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseVO handleBusinessException(BusinessException e) {
        ResponseVO responseVO = ResponseVO.getFailResponseVO(e.getMessage());
        if (e.getCode() != null) {
            responseVO.setCode(e.getCode());
        }
        return responseVO;
    }

    @ExceptionHandler(Exception.class)
    public ResponseVO handleException(Exception e) {
        log.error("服务处理异常", e);
        return ResponseVO.getFailResponseVO(ResponseCodeEnum.CODE_500.getMsg());
    }
}
