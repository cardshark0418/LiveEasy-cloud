package com.easylive.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.annotation.GlobalInterceptor;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.query.VideoInfoPostQuery;
import com.easylive.entity.vo.*;
import com.easylive.enums.ResponseCodeEnum;
import com.easylive.enums.VideoStatusEnum;
import com.easylive.exception.BusinessException;
import com.easylive.redis.RedisComponent;
import com.easylive.service.VideoInfoFilePostService;
import com.easylive.service.VideoInfoPostService;
import com.easylive.service.VideoInfoService;
import com.easylive.utils.CookieUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@Validated
@RequestMapping("/ucenter")
public class UCenterVideoPostController{
    @Resource
    private VideoInfoPostService videoInfoPostService;

    @Resource
    private VideoInfoFilePostService videoInfoFilePostService;

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private RedisComponent redisComponent;

    @RequestMapping("/postVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO postVideo(String videoId, @NotEmpty String videoCover, @NotEmpty @Size(max = 100) String videoName, @NotNull Integer pCategoryId,
                                Integer categoryId, @NotNull Integer postType, @NotEmpty @Size(max = 300) String tags, @Size(max = 2000) String introduction,
                                @Size(max = 3) String interaction, @Size(max = 500) String originInfo, @NotEmpty String uploadFileList,
                                HttpServletRequest request) {

        String token = CookieUtil.getCookieToken(request);
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        List<VideoInfoFilePost> fileInfoList = JSONUtil.toList(uploadFileList, VideoInfoFilePost.class);


        VideoInfoPost videoInfo = new VideoInfoPost();
        videoInfo.setVideoId(videoId);
        videoInfo.setVideoName(videoName);
        videoInfo.setVideoCover(videoCover);
        videoInfo.setPCategoryId(pCategoryId);
        videoInfo.setCategoryId(categoryId);
        videoInfo.setPostType(postType);
        videoInfo.setTags(tags);
        videoInfo.setIntroduction(introduction);
        videoInfo.setInteraction(interaction);
        videoInfo.setOriginInfo(originInfo);  // 设置 originInfo 字段
        videoInfo.setUserId(tokenUserInfoDto.getUserId());

        videoInfoPostService.saveVideoInfo(videoInfo, fileInfoList);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/loadVideoList")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadVideoList(Integer status, Integer pageNo, String videoNameFuzzy,HttpServletRequest request) {
        String token = CookieUtil.getCookieToken(request);
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        VideoInfoPostQuery videoInfoQuery = new VideoInfoPostQuery();
        videoInfoQuery.setUserId(tokenUserInfoDto.getUserId());
        videoInfoQuery.setOrderBy("v.create_time desc");
        videoInfoQuery.setPageNo(pageNo);
        if (status != null) {
            if (status == -1) {
                videoInfoQuery.setExcludeStatusArray(new Integer[]{VideoStatusEnum.STATUS3.getStatus(), VideoStatusEnum.STATUS4.getStatus()});
            } else {
                videoInfoQuery.setStatus(status);
            }
        }
        videoInfoQuery.setVideoNameFuzzy(videoNameFuzzy);
        videoInfoQuery.setQueryCountInfo(true);
        PaginationResultVO resultVO = videoInfoPostService.findListByPage(videoInfoQuery);
        return getSuccessResponseVO(resultVO);
    }
    @RequestMapping("/getVideoCountInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getVideoCountInfo(HttpServletRequest request) {
        String token = CookieUtil.getCookieToken(request);
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        Integer auditPassCount = Math.toIntExact(videoInfoPostService.count(new LambdaQueryWrapper<VideoInfoPost>()
                .eq(VideoInfoPost::getUserId, tokenUserInfoDto.getUserId())
                .eq(VideoInfoPost::getStatus, VideoStatusEnum.STATUS3.getStatus())));

        Integer auditFailCount = Math.toIntExact(videoInfoPostService.count(new LambdaQueryWrapper<VideoInfoPost>()
                .eq(VideoInfoPost::getUserId, tokenUserInfoDto.getUserId())
                .eq(VideoInfoPost::getStatus, VideoStatusEnum.STATUS4.getStatus())));

        int inProgress = (int) videoInfoPostService.count(new LambdaQueryWrapper<VideoInfoPost>()
                .eq(VideoInfoPost::getUserId, tokenUserInfoDto.getUserId())
                .notIn(VideoInfoPost::getStatus, VideoStatusEnum.STATUS3.getStatus(), VideoStatusEnum.STATUS4.getStatus()));

        VideoStatusCountInfoVO countInfo = new VideoStatusCountInfoVO();
        countInfo.setAuditSuccessCount(auditPassCount);
        countInfo.setAuditFailCount(auditFailCount);
        countInfo.setInProcessCount(inProgress);
        return getSuccessResponseVO(countInfo);
    }
    @RequestMapping("/getVideoByVideoId")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getVideoByVideoId(@NotEmpty String videoId,HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        VideoInfoPost videoInfoPost = this.videoInfoPostService.getById(videoId);
        if (videoInfoPost == null || !videoInfoPost.getUserId().equals(tokenUserInfoDto.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }

        List<VideoInfoFilePost> videoInfoFilePostList = this.videoInfoFilePostService.list(new LambdaQueryWrapper<VideoInfoFilePost>()
                .eq(VideoInfoFilePost::getVideoId,videoId)
                .orderByAsc(VideoInfoFilePost::getFileIndex));
        VideoPostEditInfoVo vo = new VideoPostEditInfoVo();
        vo.setVideoInfo(videoInfoPost);
        vo.setVideoInfoFileList(videoInfoFilePostList);
        return getSuccessResponseVO(vo);
    }
    @RequestMapping("/saveVideoInteraction")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO saveVideoInteraction(@NotEmpty String videoId, String interaction,HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        videoInfoService.changeInteraction(videoId, tokenUserInfoDto.getUserId(), interaction);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/deleteVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO deleteVideo(@NotEmpty String videoId,HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        videoInfoService.deleteVideo(videoId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }
}