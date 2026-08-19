package com.easylive.agent.auth;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.vo.UserLoginDto;
import com.easylive.redis.RedisUtils;
import com.easylive.utils.CookieUtil;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/** 从项目现有 HttpOnly 登录 Cookie 中解析当前用户，不信任前端传入的用户 ID。 */
@Service
public class AgentAuthService {

    private final RedisUtils redisUtils;

    public AgentAuthService(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    public UserLoginDto getCurrentUser(HttpServletRequest request) {
        String token = CookieUtil.getCookieToken(request);
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        Object value = redisUtils.get(Constants.REDIS_KEY_LOGIN_TOKEN + token);
        return value instanceof UserLoginDto ? (UserLoginDto) value : null;
    }
}
