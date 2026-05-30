package com.easylive.controller;

import com.easylive.annotation.GlobalInterceptor;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.redis.RedisComponent;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@RequestMapping("/online")
public class OnlineController{
    @Resource
    private RedisComponent redisComponent;
    @RequestMapping("/reportVideoPlayOnline")
    @GlobalInterceptor
    public ResponseVO reportVideoPlayOnline(@NotEmpty String fileId, String deviceId) {
        Integer count = redisComponent.reportVideoPlayOnline(fileId, deviceId);
        return getSuccessResponseVO(count);
    }
}