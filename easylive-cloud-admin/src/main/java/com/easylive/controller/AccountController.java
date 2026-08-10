package com.easylive.controller;

import com.easylive.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.exception.BusinessException;
import com.easylive.redis.RedisComponent;
import com.easylive.redis.RedisUtils;
import com.easylive.utils.CookieUtil;
import com.wf.captcha.ArithmeticCaptcha;
import org.springframework.util.DigestUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController("accountController")
@RequestMapping("/account")
@Validated // 控制器路由前缀
public class AccountController{ // 继承基础控制器
@Resource
private AppConfig appConfig;
	@Resource
	private RedisUtils redisUtils;



	@Resource
	private RedisComponent redisComponent; // 注入 Redis 组件

	@RequestMapping("/checkCode")
	public ResponseVO checkCode(){
		ArithmeticCaptcha captcha = new ArithmeticCaptcha(100,42);
		String ans = captcha.text();
		String checkCodeKey = UUID.randomUUID().toString();
		redisUtils.setex(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey,ans,Constants.ONE_MIN_MILLS*5);
		String checkCodeBase64 = captcha.toBase64();
		Map<String,String> result = new HashMap<>();
		result.put("checkCode",checkCodeBase64);
		result.put("checkCodeKey",checkCodeKey);
		return getSuccessResponseVO(result);
	}


	@RequestMapping("login")
	public ResponseVO login(@NotEmpty String account,
							@NotEmpty String password,
							String checkCode,
							String checkCodeKey,
							HttpServletResponse response,
							HttpServletRequest request){
		try {
			if(checkCode == null || checkCode.isEmpty() || !checkCode.equals(redisUtils.get(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey))){
				throw new BusinessException("验证码错误！");
			}
			if(!account.equals(appConfig.getAccount()) || !password.equals(DigestUtils.md5DigestAsHex(appConfig.getPassword().getBytes()))){
				throw new BusinessException("账号或密码错误！");
			}
			String token = UUID.randomUUID().toString();
			redisUtils.setex(Constants.REDIS_KEY_ADMIN_TOKEN+token,account,Constants.ONE_MIN_MILLS*60*24);
			CookieUtil.adminSetToken2Cookie(response,token);
			return ResponseVO.getSuccessResponseVO(account);
		}
		finally {
			if(checkCodeKey!=null){
				redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey);
			}
		}
	}



	@RequestMapping("/logout")
	public ResponseVO logout(HttpServletRequest request,HttpServletResponse response){
		String token = CookieUtil.adminGetCookieToken(request);
		if(token!=null){
			redisUtils.delete(Constants.REDIS_KEY_ADMIN_TOKEN+token);
			Cookie cookie = new Cookie("adminToken",null);
			cookie.setMaxAge(0);
			cookie.setPath("/");
			response.addCookie(cookie);
		}
		return ResponseVO.getSuccessResponseVO(null);
	}
}