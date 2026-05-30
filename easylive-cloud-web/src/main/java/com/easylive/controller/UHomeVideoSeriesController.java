package com.easylive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.annotation.GlobalInterceptor;
import com.easylive.entity.po.UserVideoSeries;
import com.easylive.entity.po.UserVideoSeriesVideo;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.UserLoginDto;
import com.easylive.entity.vo.UserVideoSeriesDetailVO;
import com.easylive.enums.ResponseCodeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.redis.RedisComponent;
import com.easylive.service.UserVideoSeriesService;
import com.easylive.service.UserVideoSeriesVideoService;
import com.easylive.service.VideoInfoService;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.stream.Collectors;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@Validated
@RequestMapping("/uhome/series")
public class UHomeVideoSeriesController {

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private UserVideoSeriesService userVideoSeriesService;

    @Resource
    private UserVideoSeriesVideoService userVideoSeriesVideoService;

    @Autowired
    private RedisComponent redisComponent;

    @RequestMapping("/loadVideoSeries")
    //
    public ResponseVO loadVideoSeries(@NotEmpty String userId) {
        List<UserVideoSeries> videoSeries = userVideoSeriesService.selectJoinList(
                UserVideoSeries.class,
                new MPJLambdaWrapper<UserVideoSeries>()
                        .selectAll(UserVideoSeries.class) // 查合集信息
                        .selectCollection(VideoInfo.class, UserVideoSeries::getVideoInfoList) // 关键：自动封装关联的视频列表
                        .leftJoin(UserVideoSeriesVideo.class, UserVideoSeriesVideo::getSeriesId, UserVideoSeries::getSeriesId)
                        .leftJoin(VideoInfo.class, VideoInfo::getVideoId, UserVideoSeriesVideo::getVideoId)
                        .eq(UserVideoSeries::getUserId, userId)
        );
        return getSuccessResponseVO(videoSeries);
    }

    /**
     * 保存系列
     *
     * @param seriesId
     * @param seriesName
     * @param seriesDescription
     * @param videoIds
     * @return
     */
    @RequestMapping("/saveVideoSeries")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO saveVideoSeries(Integer seriesId,
                                      @NotEmpty @Size(max = 100) String seriesName,
                                      @Size(max = 200) String seriesDescription,
                                      String videoIds,
                                      HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        UserVideoSeries videoSeries = new UserVideoSeries();
        videoSeries.setUserId(tokenUserInfoDto.getUserId());
        videoSeries.setSeriesId(seriesId);
        videoSeries.setSeriesName(seriesName);
        videoSeries.setSeriesDescription(seriesDescription);
        userVideoSeriesService.saveUserVideoSeries(videoSeries, videoIds);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadAllVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadAllVideo(Integer seriesId, HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        LambdaQueryWrapper<VideoInfo> wrapper = new LambdaQueryWrapper<VideoInfo>().eq(VideoInfo::getUserId, tokenUserInfoDto.getUserId());
        if (seriesId != null) {
            List<UserVideoSeriesVideo> seriesVideoList = userVideoSeriesVideoService.list(new LambdaQueryWrapper<UserVideoSeriesVideo>()
                    .eq(UserVideoSeriesVideo::getSeriesId,seriesId)
                    .eq(UserVideoSeriesVideo::getUserId,tokenUserInfoDto.getUserId()));
            List<String> videoList = seriesVideoList.stream().map(item -> item.getVideoId()).collect(Collectors.toList());
            wrapper.notIn(VideoInfo::getVideoId,videoList);
        }
        List<VideoInfo> videoInfoList = videoInfoService.list(wrapper);
        return getSuccessResponseVO(videoInfoList);
    }

    @RequestMapping("/getVideoSeriesDetail")
//    @GlobalInterceptor
    public ResponseVO getVideoSeriesDetail(@NotNull Integer seriesId) {
        UserVideoSeries videoSeries = userVideoSeriesService.getById(seriesId);
        if (videoSeries == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        List<UserVideoSeriesVideo> seriesVideoList = userVideoSeriesVideoService.selectJoinList(UserVideoSeriesVideo.class,new MPJLambdaWrapper<UserVideoSeriesVideo>()
                .eq(UserVideoSeriesVideo::getSeriesId,seriesId)
                .orderByAsc(UserVideoSeriesVideo::getSort)
                .leftJoin(VideoInfo.class,VideoInfo::getVideoId,UserVideoSeriesVideo::getVideoId)
                .selectAll(UserVideoSeriesVideo.class)
                .select(VideoInfo::getVideoCover,VideoInfo::getVideoName,VideoInfo::getPlayCount,VideoInfo::getCreateTime));
        return getSuccessResponseVO(new UserVideoSeriesDetailVO(videoSeries, seriesVideoList));
    }

    /**
     * 保存系列视频
     *
     * @param seriesId
     * @param videoIds
     * @return
     */
    @RequestMapping("/saveSeriesVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO saveSeriesVideo(@NotNull Integer seriesId, @NotEmpty String videoIds,HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        userVideoSeriesService.saveSeriesVideo(tokenUserInfoDto.getUserId(), seriesId, videoIds);
        return getSuccessResponseVO(null);
    }

    /**
     * 删除视频
     *
     * @param seriesId
     * @param videoId
     * @return
     */
    @RequestMapping("/delSeriesVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delSeriesVideo(@NotNull Integer seriesId, @NotEmpty String videoId,HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        userVideoSeriesService.delSeriesVideo(tokenUserInfoDto.getUserId(), seriesId, videoId);
        return getSuccessResponseVO(null);
    }

    /**
     * 删除系列
     *
     */
    @RequestMapping("/delVideoSeries")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delVideoSeries(@NotNull Integer seriesId,HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        userVideoSeriesService.delVideoSeries(tokenUserInfoDto.getUserId(), seriesId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadVideoSeriesWithVideo")
//    @GlobalInterceptor
    public ResponseVO loadVideoSeriesWithVideo(@NotEmpty String userId) {
        List<UserVideoSeries> videoSeries = userVideoSeriesService.selectJoinList(UserVideoSeries.class,new MPJLambdaWrapper<UserVideoSeries>()
                .selectAll(UserVideoSeries.class)
                .selectCollection(VideoInfo.class,UserVideoSeries::getVideoInfoList)
                .eq(UserVideoSeries::getUserId,userId)
                .orderByAsc(UserVideoSeries::getSort)
                .leftJoin(UserVideoSeriesVideo.class,UserVideoSeriesVideo::getSeriesId,UserVideoSeries::getSeriesId)
                .leftJoin(VideoInfo.class,VideoInfo::getVideoId,UserVideoSeriesVideo::getVideoId));
        return getSuccessResponseVO(videoSeries);
    }

    @RequestMapping("/changeVideoSeriesSort")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO changeVideoSeriesSort(@NotEmpty String seriesIds,HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        userVideoSeriesService.changeVideoSeriesSort(tokenUserInfoDto.getUserId(), seriesIds);
        return getSuccessResponseVO(null);
    }
}