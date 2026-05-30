package com.easylive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.entity.po.UserVideoSeries;
import com.easylive.entity.po.UserVideoSeriesVideo;
import com.easylive.entity.po.VideoInfo;
import com.easylive.enums.ResponseCodeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.mapper.UserVideoSeriesMapper;
import com.easylive.mapper.UserVideoSeriesVideoMapper;
import com.easylive.mapper.VideoInfoMapper;
import com.easylive.service.UserVideoSeriesService;
import com.easylive.service.UserVideoSeriesVideoService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class UserVideoSeriesServiceImpl extends MPJBaseServiceImpl<UserVideoSeriesMapper, UserVideoSeries> implements UserVideoSeriesService {

    @Autowired
    private UserVideoSeriesMapper userVideoSeriesMapper;
    @Autowired
    private VideoInfoMapper videoInfoMapper;
    @Autowired
    private UserVideoSeriesVideoMapper userVideoSeriesVideoMapper;
    @Autowired
    private UserVideoSeriesVideoService userVideoSeriesVideoService;

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void saveUserVideoSeries(UserVideoSeries bean, String videoIds) {
        if (bean.getSeriesId() == null && StringUtils.isEmpty(videoIds)) {
            throw new BusinessException(String.valueOf(ResponseCodeEnum.CODE_600));
        }
        if (bean.getSeriesId() == null) {//新合集
            checkVideoIds(bean.getUserId(), videoIds);
            bean.setUpdateTime(new Date());
            Integer maxSort = this.userVideoSeriesMapper.selectJoinOne(Integer.class,new MPJLambdaWrapper<UserVideoSeries>()
                    .selectMax(UserVideoSeries::getSort)
                    .eq(UserVideoSeries::getUserId,bean.getUserId()));
            bean.setSort((maxSort == null ? 0 : maxSort) + 1);
            userVideoSeriesMapper.insert(bean);
            saveSeriesVideo(bean.getUserId(), bean.getSeriesId(), videoIds);
        } else {//更新
            this.userVideoSeriesMapper.update(bean, new LambdaQueryWrapper<UserVideoSeries>()
                    .eq(UserVideoSeries::getUserId,bean.getUserId())
                    .eq(UserVideoSeries::getSeriesId,bean.getSeriesId()));
            saveSeriesVideo(bean.getUserId(), bean.getSeriesId(), videoIds);
        }
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class) // 建议加上事务，保证删和增要么都成功，要么都失败
    public void saveSeriesVideo(String userId, Integer seriesId, String videoIds) {
        UserVideoSeries userVideoSeries = getById(seriesId);
        if (userVideoSeries == null || !userVideoSeries.getUserId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        checkVideoIds(userId, videoIds);
        String[] videoIdArray = videoIds.split(",");

        // 1. 【关键改动】先删除该合集下这些视频的旧关联
        // 这样可以防止你插入 V1, V2 时，因为数据库已经有 V1 而导致主键冲突报错
        userVideoSeriesVideoService.remove(new LambdaQueryWrapper<UserVideoSeriesVideo>()
                .eq(UserVideoSeriesVideo::getSeriesId, seriesId)
                .in(UserVideoSeriesVideo::getVideoId, Arrays.asList(videoIdArray)));

        // 2. 获取当前的 Max Sort（保持你原有的逻辑）
        Integer sort = this.userVideoSeriesVideoMapper.selectJoinOne(Integer.class, new MPJLambdaWrapper<UserVideoSeriesVideo>()
                .selectMax(UserVideoSeriesVideo::getSort)
                .eq(UserVideoSeriesVideo::getSeriesId, seriesId));
        sort = (sort == null) ? 0 : sort;

        // 3. 构建待插入列表
        List<UserVideoSeriesVideo> seriesVideoList = new ArrayList<>();
        for (String videoId : videoIdArray) {
            UserVideoSeriesVideo videoSeriesVideo = new UserVideoSeriesVideo();
            videoSeriesVideo.setVideoId(videoId);
            videoSeriesVideo.setSort(++sort);
            videoSeriesVideo.setSeriesId(seriesId);
            videoSeriesVideo.setUserId(userId);
            seriesVideoList.add(videoSeriesVideo);
        }

        // 4. 【关键改动】使用 saveBatch 而不是 saveOrUpdateBatch
        // 因为前面的 remove 已经清理了战场，这里直接 INSERT 即可，绝对不会只剩一个
        if (!seriesVideoList.isEmpty()) {
            this.userVideoSeriesVideoService.saveBatch(seriesVideoList);
        }
    }

    @Override
    public void delSeriesVideo(String userId, Integer seriesId, String videoId) {
        this.userVideoSeriesVideoMapper.delete(new LambdaQueryWrapper<UserVideoSeriesVideo>()
                .eq(UserVideoSeriesVideo::getUserId,userId)
                .eq(UserVideoSeriesVideo::getSeriesId,seriesId)
                .eq(UserVideoSeriesVideo::getVideoId,videoId));
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void delVideoSeries(String userId, Integer seriesId) {
        int count = userVideoSeriesMapper.delete(new LambdaQueryWrapper<UserVideoSeries>()
                .eq(UserVideoSeries::getUserId,userId)
                .eq(UserVideoSeries::getSeriesId,seriesId));
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        userVideoSeriesVideoMapper.delete(new LambdaQueryWrapper<UserVideoSeriesVideo>()
                .eq(UserVideoSeriesVideo::getSeriesId,seriesId)
                .eq(UserVideoSeriesVideo::getUserId,userId));
    }

    @Override
    public void changeVideoSeriesSort(String userId, String seriesIds) {
        String[] seriesIdArray = seriesIds.split(",");
        List<UserVideoSeries> videoSeriesList = new ArrayList<>();
        Integer sort = 0;
        for (String seriesId : seriesIdArray) {
            UserVideoSeries videoSeries = new UserVideoSeries();
            videoSeries.setUserId(userId);
            videoSeries.setSeriesId(Integer.parseInt(seriesId));
            videoSeries.setSort(++sort);
            videoSeriesList.add(videoSeries);
        }
        userVideoSeriesMapper.changeSort(videoSeriesList);
    }



    //校验视频id
    private void checkVideoIds(String userId, String videoIds) {
        String[] videoIdArray = videoIds.split(",");
        Integer count = Math.toIntExact(videoInfoMapper.selectCount(new LambdaQueryWrapper<VideoInfo>()
                .eq(VideoInfo::getUserId, userId)
                .in(VideoInfo::getVideoId, videoIdArray)));
        if (videoIdArray.length != count) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }
}