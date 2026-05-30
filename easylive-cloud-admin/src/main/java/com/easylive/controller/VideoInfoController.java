package com.easylive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.annotation.RecordUserMessage;
import com.easylive.api.consumer.WebClient;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.query.VideoInfoPostQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.enums.MessageTypeEnum;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@Validated
@RequestMapping("/videoInfo")
public class VideoInfoController{
    @Resource
    private WebClient webClient;

    @RequestMapping("/loadVideoList")
    public ResponseVO loadVideoList(VideoInfoPostQuery videoInfoPostQuery) {
        videoInfoPostQuery.setOrderBy("last_update_time desc");
        videoInfoPostQuery.setQueryCountInfo(true);
        videoInfoPostQuery.setQueryUserInfo(true);
        PaginationResultVO resultVO = webClient.loadVideoList(videoInfoPostQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/auditVideo")
    @RecordUserMessage(messageType = MessageTypeEnum.SYS)
    public ResponseVO auditVideo(@NotEmpty String videoId, @NotNull Integer status, String reason) {
        webClient.auditVideo(videoId, status, reason);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/recommendVideo")
    public ResponseVO recommendVideo(@NotEmpty String videoId) {
        webClient.recommendVideo(videoId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/deleteVideo")
    public ResponseVO deleteVideo(@NotEmpty String videoId) {
        webClient.deleteVideo(videoId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadVideoPList")
    public ResponseVO loadVideoPList(@NotEmpty String videoId) {
        List<VideoInfoFilePost> videoInfoFilePostsList = webClient.loadVideoPList(videoId);
        return getSuccessResponseVO(videoInfoFilePostsList);
    }
}
