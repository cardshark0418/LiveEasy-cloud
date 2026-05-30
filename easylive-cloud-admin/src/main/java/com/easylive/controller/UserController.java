package com.easylive.controller;

import com.easylive.api.consumer.WebClient;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.redis.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@RequestMapping("/user")
@Validated
public class UserController{
    @Resource
    private WebClient webClient;
    @Autowired
    private RedisUtils redisUtils;

    @RequestMapping("/loadUser")
    public ResponseVO loadUser(Integer pageNo, String nickNameFuzzy, Integer status) {
        pageNo= pageNo==null?1:pageNo;
        return getSuccessResponseVO(webClient.loadUser(pageNo,nickNameFuzzy,status));
    }

    @RequestMapping("/changeStatus")
    public ResponseVO changeStatus(String userId, Integer status) {
        webClient.changeStatus(userId,status);
        return getSuccessResponseVO(null);
    }
}