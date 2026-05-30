package com.easylive.controller;

import com.easylive.entity.vo.ResponseVO;
import com.easylive.redis.RedisComponent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController("sysSettingController")
@RequestMapping("/sysSetting")
@Validated
public class SysSettingController {

    @Resource
    private RedisComponent redisComponent;

    @RequestMapping(value = "/getSetting")
//    @GlobalInterceptor
    public ResponseVO getSetting() {
        return getSuccessResponseVO(redisComponent.getSysSettingDto());
    }
}