package com.easylive.controller;

import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.SysSettingDto;
import com.easylive.redis.RedisComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@Validated
@Slf4j
@RestController
@RequestMapping("/setting")
public class SettingController {

    @Resource
    private RedisComponent redisComponent;

    @RequestMapping("/getSetting")
    public ResponseVO getSetting() {
        return getSuccessResponseVO(redisComponent.getSysSettingDto());
    }


    @RequestMapping("/saveSetting")
    public ResponseVO saveSetting(SysSettingDto sysSettingDto) {
        redisComponent.saveSettingDto(sysSettingDto);
        return getSuccessResponseVO(null);
    }
}