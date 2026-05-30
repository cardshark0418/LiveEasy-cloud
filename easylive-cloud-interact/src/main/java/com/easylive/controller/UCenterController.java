package com.easylive.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easylive.annotation.GlobalInterceptor;
import com.easylive.api.consumer.UserClient;
import com.easylive.api.consumer.VideoClient;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.po.VideoComment;
import com.easylive.entity.po.VideoDanmu;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.UserLoginDto;
import com.easylive.redis.RedisComponent;
import com.easylive.service.VideoCommentService;
import com.easylive.service.VideoDanmuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@Validated
@RequestMapping("/ucenter")
@GlobalInterceptor(checkLogin = true)
public class UCenterController {

    @Resource
    private VideoCommentService videoCommentService;

    @Resource
    private VideoDanmuService videoDanmuService;


    @Autowired
    private RedisComponent redisComponent;

    @Autowired
    private VideoClient videoClient;

    @Autowired
    private UserClient userClient;


    @RequestMapping("/loadComment")
    public ResponseVO loadComment(Integer pageNo, String videoId, HttpServletRequest request) {
        pageNo = pageNo == null ? 1 : pageNo;
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);

        // 1. 仅查询评论表（去除 Cross-Service JOIN）
        LambdaQueryWrapper<VideoComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoComment::getUserId, tokenUserInfoDto.getUserId()) // 假设是查“我的评论”
                .orderByDesc(VideoComment::getCommentId);

        if (!StrUtil.isBlank(videoId)) {
            wrapper.eq(VideoComment::getVideoId, videoId);
        }

        // 执行分页查询
        Page<VideoComment> page = videoCommentService.page(new Page<>(pageNo, 15), wrapper);
        List<VideoComment> commentList = page.getRecords();

        // 2. 批量获取视频信息（核心：应用层组装）
        if (!commentList.isEmpty()) {
            // 提取所有 videoId 集合
            Set<String> videoIds = commentList.stream()
                    .map(VideoComment::getVideoId)
                    .collect(Collectors.toSet());

            Map<String, VideoInfo> videoInfoMap = videoClient.getVideoInfoBatch(videoIds);

            if (videoInfoMap == null) {
                videoInfoMap = new HashMap<>();
            }

            // 3. 填充数据
            Map<String, VideoInfo> finalVideoInfoMap = videoInfoMap;
            commentList.forEach(comment -> {
                VideoInfo video = finalVideoInfoMap.get(comment.getVideoId());
                if (video != null) {
                    comment.setVideoName(video.getVideoName());
                    comment.setVideoCover(video.getVideoCover());
                }
            });
        }

        return getSuccessResponseVO(new PaginationResultVO<>(
                (int) page.getTotal(), 15, pageNo, (int) page.getPages(), commentList));
    }

    @RequestMapping("/delComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delComment(@NotNull Integer commentId, HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        videoCommentService.deleteComment(commentId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadDanmu")
    public ResponseVO loadDanmu(Integer pageNo, String videoId, HttpServletRequest request) {
        pageNo = pageNo == null ? 1 : pageNo;
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);

        // 1. 纯单表查询弹幕（去除所有 leftJoin 和 selectAs）
        LambdaQueryWrapper<VideoDanmu> wrapper = new LambdaQueryWrapper<>();

        String userId = tokenUserInfoDto.getUserId();
        List<String> myVideoIds = videoClient.getVideoIdsByUserId(userId);
        if (CollUtil.isEmpty(myVideoIds)) {
            return getSuccessResponseVO(new PaginationResultVO<>(0, 15, pageNo, new ArrayList<>()));
        }

        wrapper.in(VideoDanmu::getVideoId, myVideoIds);
        wrapper.orderByDesc(VideoDanmu::getDanmuId);

        if (StrUtil.isNotBlank(videoId)) {
            wrapper.eq(VideoDanmu::getVideoId, videoId);
        }

        // 执行分页查询
        Page<VideoDanmu> page = videoDanmuService.page(new Page<>(pageNo, 15), wrapper);
        List<VideoDanmu> danmuList = page.getRecords();

        // 2. 异步/批量补全数据（视频信息 + 用户信息）
        if (CollUtil.isNotEmpty(danmuList)) {
            // 提取 ID 集合
            Set<String> videoIds = danmuList.stream().map(VideoDanmu::getVideoId).collect(Collectors.toSet());
            Set<String> userIds = danmuList.stream().map(VideoDanmu::getUserId).collect(Collectors.toSet());

            Map<String, VideoInfo> videoMap = videoClient.getVideoInfoBatch(videoIds);
            Map<String, UserInfo> userMap = userClient.getUserInfoBatch(userIds);

            // 3. 内存组装数据
            danmuList.forEach(danmu -> {
                // 补全视频信息
                if (videoMap != null && videoMap.containsKey(danmu.getVideoId())) {
                    VideoInfo v = videoMap.get(danmu.getVideoId());
                    danmu.setVideoName(v.getVideoName());
                    danmu.setVideoCover(v.getVideoCover());
                }
                // 补全用户信息
                if (userMap != null && userMap.containsKey(danmu.getUserId())) {
                    UserInfo u = userMap.get(danmu.getUserId());
                    danmu.setNickName(u.getNickName());
                }
            });
        }

        return getSuccessResponseVO(new PaginationResultVO<>((int) page.getTotal(), 15, pageNo, danmuList));
    }

    @RequestMapping("/delDanmu")
//    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delDanmu(@NotNull Integer danmuId,HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        videoDanmuService.deleteDanmu(tokenUserInfoDto.getUserId(), danmuId);
        return getSuccessResponseVO(null);
    }
}