package com.easylive.aspect;

import cn.hutool.core.util.StrUtil;
import com.easylive.entity.vo.UserLoginDto;
import com.easylive.enums.ResponseCodeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.redis.RedisComponent;
import com.easylive.redis.RedisUtils;
import com.easylive.utils.CookieUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.easylive.annotation.GlobalInterceptor;
import java.lang.reflect.Method;
import org.aspectj.lang.reflect.MethodSignature;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Component("operationAspect")
@Aspect
@Slf4j
public class GlobalOperationAspect {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private RedisComponent redisComponent;

    @Before("@annotation(com.easylive.annotation.GlobalInterceptor)")
    public void interceptorDo(JoinPoint point) {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
        if (null == interceptor) {
            return;
        }
        if (interceptor.checkLogin()) {
            checkLogin();
        }
    }
    //校验登录
    private void checkLogin() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = CookieUtil.getCookieToken(request);
        if (StrUtil.isEmpty(token)) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        if (tokenUserInfoDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
    }
}