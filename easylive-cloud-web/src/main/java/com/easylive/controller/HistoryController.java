package com.easylive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easylive.annotation.GlobalInterceptor;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoPlayHistory;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.UserLoginDto;
import com.easylive.redis.RedisComponent;
import com.easylive.service.VideoInfoService;
import com.easylive.service.VideoPlayHistoryService;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@Validated
@RequestMapping("/history")
public class HistoryController {

    @Resource
    private VideoPlayHistoryService videoPlayHistoryService;
    @Resource
    private VideoInfoService videoInfoService;
    @Resource
    private RedisComponent redisComponent;

    @RequestMapping("/loadHistory")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadHistory(Integer pageNo, HttpServletRequest request) {
        pageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        Page<VideoPlayHistory> page = videoPlayHistoryService.selectJoinListPage(
                new Page<>(pageNo, 20),
                VideoPlayHistory.class,
                new MPJLambdaWrapper<VideoPlayHistory>()
                        .selectAll(VideoPlayHistory.class)
                        .selectAs(VideoInfo::getVideoName, VideoPlayHistory::getVideoName)
                        .selectAs(VideoInfo::getVideoCover, VideoPlayHistory::getVideoCover)
                        .leftJoin(VideoInfo.class, VideoInfo::getVideoId, VideoPlayHistory::getVideoId)
                        .eq(VideoPlayHistory::getUserId, tokenUserInfoDto.getUserId())
                        .orderByDesc(VideoPlayHistory::getLastUpdateTime));
        PaginationResultVO<VideoPlayHistory> resultVO = new PaginationResultVO<>(
                (int) page.getTotal(), 20, pageNo, page.getRecords());
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/cleanAllHistory")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO cleanAllHistory(HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        videoPlayHistoryService.remove(new LambdaQueryWrapper<VideoPlayHistory>()
                .eq(VideoPlayHistory::getUserId, tokenUserInfoDto.getUserId()));
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/delHistory")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delHistory(@NotEmpty String videoId, HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        videoPlayHistoryService.remove(new LambdaQueryWrapper<VideoPlayHistory>()
                .eq(VideoPlayHistory::getUserId, tokenUserInfoDto.getUserId())
                .eq(VideoPlayHistory::getVideoId, videoId));
        return getSuccessResponseVO(null);
    }
}
