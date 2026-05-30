package com.easylive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.annotation.GlobalInterceptor;
import com.easylive.api.consumer.VideoClient;
import com.easylive.entity.po.VideoDanmu;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.redis.RedisComponent;
import com.easylive.service.VideoDanmuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@Validated
@RequestMapping("/danmu")
@Slf4j
public class VideoDanmuController{

    @Resource
    private VideoDanmuService videoDanmuService;

    @Resource
    private VideoClient videoClient;

    @Resource
    private RedisComponent redisComponent;

    @RequestMapping("/loadDanmu")
//    @GlobalInterceptor
    public ResponseVO loadDanmu(@NotEmpty String fileId, @NotEmpty String videoId) {

        VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(videoId);
        if (videoInfo.getInteraction() != null && videoInfo.getInteraction().contains("0")) {
            return getSuccessResponseVO(new ArrayList<>());
        }

        return getSuccessResponseVO(videoDanmuService.list(new LambdaQueryWrapper<VideoDanmu>()
                .eq(VideoDanmu::getFileId,fileId)
                .orderByAsc(VideoDanmu::getDanmuId)));
    }

    @RequestMapping("/postDanmu")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO postDanmu(@NotEmpty String videoId,
                                @NotEmpty String fileId,
                                @NotEmpty @Size(max = 200) String text,
                                @NotNull Integer mode,
                                @NotEmpty String color,
                                @NotNull Integer time,
                                HttpServletRequest request) {

        VideoDanmu videoDanmu = new VideoDanmu();
        videoDanmu.setVideoId(videoId);
        videoDanmu.setFileId(fileId);
        videoDanmu.setText(text);
        videoDanmu.setMode(mode);
        videoDanmu.setColor(color);
        videoDanmu.setTime(time);
        videoDanmu.setUserId(redisComponent.getTokenUserInfoDto(request).getUserId());
        videoDanmu.setPostTime(new Date());
        videoDanmuService.saveDanmu(videoDanmu);
        return getSuccessResponseVO(null);
    }
}