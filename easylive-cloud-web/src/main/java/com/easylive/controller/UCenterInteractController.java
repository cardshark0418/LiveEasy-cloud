package com.easylive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easylive.annotation.GlobalInterceptor;
import com.easylive.entity.po.VideoComment;
import com.easylive.entity.po.VideoDanmu;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.UserLoginDto;
import com.easylive.redis.RedisComponent;
import com.easylive.service.VideoInfoService;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.List;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@Validated
@RequestMapping("/ucenter")
@GlobalInterceptor(checkLogin = true)
public class UCenterInteractController{


    @Resource
    private VideoInfoService videoInfoService;

    @Autowired
    private RedisComponent redisComponent;

    @RequestMapping("/loadAllVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadAllVideo(HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        List<VideoInfo> videoInfoList = videoInfoService.list(new LambdaQueryWrapper<VideoInfo>()
                .eq(VideoInfo::getUserId,tokenUserInfoDto.getUserId())
                .orderByDesc(VideoInfo::getCreateTime));
        return getSuccessResponseVO(videoInfoList);
    }

}