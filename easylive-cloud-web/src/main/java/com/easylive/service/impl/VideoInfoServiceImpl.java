package com.easylive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.easylive.api.consumer.UserActionClient;
import com.easylive.component.EsSearchComponent;
import com.easylive.config.AppConfig;
import com.easylive.entity.po.*;
import com.easylive.entity.vo.SysSettingDto;
import com.easylive.enums.ResponseCodeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.mapper.*;
import com.easylive.redis.RedisComponent;
import com.easylive.service.UserInfoService;
import com.easylive.service.VideoInfoService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class VideoInfoServiceImpl extends MPJBaseServiceImpl<VideoInfoMapper, VideoInfo> implements VideoInfoService {


    @Resource
    private AppConfig appConfig;

    @Resource
    private VideoInfoMapper videoInfoMapper;

    @Resource
    private VideoInfoPostMapper videoInfoPostMapper;



    @Resource
    private VideoInfoFileMapper videoInfoFileMapper;

    @Resource
    private VideoInfoFilePostMapper videoInfoFilePostMapper;

    @Resource
    private UserActionClient interactClient;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private EsSearchComponent esSearchComponent;

    private static ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void changeInteraction(String videoId, String userId, String interaction) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setInteraction(interaction);
        videoInfoMapper.update(videoInfo, new LambdaQueryWrapper<VideoInfo>()
                .eq(VideoInfo::getUserId,userId)
                .eq(VideoInfo::getVideoId,videoId));

        VideoInfoPost videoInfoPost = new VideoInfoPost();
        videoInfoPost.setInteraction(interaction);
        videoInfoPostMapper.update(videoInfoPost, new LambdaQueryWrapper<VideoInfoPost>()
                .eq(VideoInfoPost::getUserId,userId)
                .eq(VideoInfoPost::getVideoId,videoId));
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void deleteVideo(String videoId, String userId) {
        VideoInfoPost videoInfoPost = this.videoInfoPostMapper.selectById(videoId);
        if (videoInfoPost == null || userId != null && !userId.equals(videoInfoPost.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        this.videoInfoMapper.deleteById(videoId);
        this.videoInfoPostMapper.deleteById(videoId);

        /**
         * 删除用户硬币
         */
        SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
        userInfoService.update(new LambdaUpdateWrapper<UserInfo>()
                .eq(UserInfo::getUserId,userId)
                .setSql("total_coin_count = total_coin_count - " + sysSettingDto.getPostVideoCoinCount().toString()));
        /**
         * 删除es信息
         */
        esSearchComponent.delDoc(videoId);

        executorService.execute(() -> {

            LambdaQueryWrapper<VideoInfoFile> videoInfoFileWrapper = new LambdaQueryWrapper<VideoInfoFile>()
                    .eq(VideoInfoFile::getVideoId, videoId);
            //查询分P
            List<VideoInfoFile> videoInfoFileList = videoInfoFileMapper.selectList(videoInfoFileWrapper);
            //删除分P
            videoInfoFileMapper.delete(videoInfoFileWrapper);

            videoInfoFilePostMapper.delete(new LambdaQueryWrapper<VideoInfoFilePost>()
                    .eq(VideoInfoFilePost::getVideoId,videoId));
            //删除弹幕
            interactClient.deleteVideoDanmu(videoId);
            //删除评论
            interactClient.deleteVideoComment(videoId);
            //删除文件
            for (VideoInfoFile item : videoInfoFileList) {
                try {
                    FileUtils.deleteDirectory(new File(appConfig.getProjectFolder() + item.getFilePath()));
                } catch (IOException e) {
                    log.error("删除文件失败，文件路径:"+item.getFilePath());
                }
            }
        });
    }
}
