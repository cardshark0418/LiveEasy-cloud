package com.easylive.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.api.consumer.UserActionClient;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.*;
import com.easylive.enums.StatisticsTypeEnum;
import com.easylive.enums.UserActionTypeEnum;
import com.easylive.mapper.*;
import com.easylive.mapper.StatisticsInfoMapper;
import com.easylive.mapper.UserFocusMapper;
import com.easylive.mapper.VideoInfoMapper;
import com.easylive.redis.RedisComponent;
import com.easylive.service.StatisticsInfoService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsInfoServiceImpl extends MPJBaseServiceImpl<StatisticsInfoMapper, StatisticsInfo> implements StatisticsInfoService {

    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private StatisticsInfoMapper statisticsInfoMapper;
    @Autowired
    private VideoInfoMapper videoInfoMapper;
    @Autowired
    private UserFocusMapper userFocusMapper;
    @Autowired
    private UserActionClient interactClient;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Override
    public void statisticsData() {
        List<StatisticsInfo> statisticsInfoList = new ArrayList<>();
        DateTime yesterday = DateUtil.yesterday();

        String statisticsDate = DateUtil.format(yesterday, "yyyyMMdd");
        //统计播放量
        Map<String, Object> videoPlayCountMap = redisComponent.getVideoPlayCount(statisticsDate);
        List<String> playVideoKeys = new ArrayList<>(videoPlayCountMap.keySet());
        playVideoKeys = playVideoKeys.stream().map(item -> item.substring(item.lastIndexOf(":") + 1)).collect(Collectors.toList());
        //全站昨天有播放的视频列表
        List<VideoInfo> videoInfoList = videoInfoMapper.selectList(new LambdaQueryWrapper<VideoInfo>()
                .in(!playVideoKeys.isEmpty(),VideoInfo::getVideoId,playVideoKeys.toArray(new String[playVideoKeys.size()])));

        //根据每个up主分类 key为up主的userId val为昨日播放量
        Map<String, Integer> videoCountMap = videoInfoList.stream().collect(Collectors.groupingBy(VideoInfo::getUserId,
                Collectors.summingInt(item -> {
                    Integer count = (Integer) videoPlayCountMap.get(Constants.REDIS_KEY_VIDEO_PLAY_COUNT + statisticsDate + ":" + item.getVideoId());
                    return count == null ? 0 : count;
                })));
        videoCountMap.forEach((k, v) -> {
            StatisticsInfo statisticsInfo = new StatisticsInfo();
            statisticsInfo.setStatisticsDate(statisticsDate);
            statisticsInfo.setUserId(k);
            statisticsInfo.setDataType(StatisticsTypeEnum.PLAY.getType());
            statisticsInfo.setStatisticsCount(v);
            statisticsInfoList.add(statisticsInfo);
        });

        //统计粉丝量
        List<StatisticsInfo> fansDataList = selectStatisticsFans(statisticsDate);
        for (StatisticsInfo statisticsInfo : fansDataList) {
            statisticsInfo.setStatisticsDate(statisticsDate);
            statisticsInfo.setDataType(StatisticsTypeEnum.FANS.getType());
        }
        statisticsInfoList.addAll(fansDataList);

        //统计评论
        List<StatisticsInfo> commentDataList = selectStatisticsComment(statisticsDate);
        for (StatisticsInfo statisticsInfo : commentDataList) {
            statisticsInfo.setStatisticsDate(statisticsDate);
            statisticsInfo.setDataType(StatisticsTypeEnum.COMMENT.getType());
        }
        statisticsInfoList.addAll(commentDataList);

        //统计 弹幕、点赞、收藏、投币
        List<StatisticsInfo> statisticsInfoOthers = selectStatisticsInfo(statisticsDate,
                new Integer[]{UserActionTypeEnum.VIDEO_LIKE.getType(), UserActionTypeEnum.VIDEO_COIN.getType(), UserActionTypeEnum.VIDEO_COLLECT.getType()});

        for (StatisticsInfo statisticsInfo : statisticsInfoOthers) {
            statisticsInfo.setStatisticsDate(statisticsDate);
            if (UserActionTypeEnum.VIDEO_LIKE.getType().equals(statisticsInfo.getDataType())) {
                statisticsInfo.setDataType(StatisticsTypeEnum.LIKE.getType());
            } else if (UserActionTypeEnum.VIDEO_COLLECT.getType().equals(statisticsInfo.getDataType())) {
                statisticsInfo.setDataType(StatisticsTypeEnum.COLLECTION.getType());
            } else if (UserActionTypeEnum.VIDEO_COIN.getType().equals(statisticsInfo.getDataType())) {
                statisticsInfo.setDataType(StatisticsTypeEnum.COIN.getType());
            }
        }
        statisticsInfoList.addAll(statisticsInfoOthers);

//        saveOrUpdateBatch(statisticsInfoList);
        statisticsInfoMapper.delete(new LambdaQueryWrapper<StatisticsInfo>()
                .eq(StatisticsInfo::getStatisticsDate, yesterday));

        saveBatch(statisticsInfoList);
    }

    public List<StatisticsInfo> selectStatisticsFans(String statisticsDate) {
        String dateDash = statisticsDate.substring(0, 4) + "-" + statisticsDate.substring(4, 6) + "-" + statisticsDate.substring(6, 8);
        String begin = dateDash + " 00:00:00";
        String end = dateDash + " 23:59:59";

        return userFocusMapper.selectJoinList(StatisticsInfo.class,
                new MPJLambdaWrapper<UserFocus>()
                        .selectAs(UserFocus::getFocusUserId, StatisticsInfo::getUserId)
                        .selectCount(UserFocus::getFocusUserId, StatisticsInfo::getStatisticsCount)
                        .between(UserFocus::getFocusTime, begin, end)
                        .groupBy(UserFocus::getFocusUserId)
        );
    }

    public List<StatisticsInfo> selectStatisticsComment(String statisticsDate) {
        String dateDash = statisticsDate.substring(0, 4) + "-" + statisticsDate.substring(4, 6) + "-" + statisticsDate.substring(6, 8);
        String begin = dateDash + " 00:00:00";
        String end = dateDash + " 23:59:59";
        return interactClient.selectStatisticsComment(begin, end);
    }

    public List<StatisticsInfo> selectStatisticsInfo(String statisticsDate, Integer[] actionTypes) {
        String dateDash = statisticsDate.substring(0, 4) + "-" + statisticsDate.substring(4, 6) + "-" + statisticsDate.substring(6, 8);
        String begin = dateDash + " 00:00:00";
        String end = dateDash + " 23:59:59";
        return interactClient.selectStatisticsInfo(begin, end,actionTypes);
    }

    @Override
    public Map<String, Object> getStatisticsInfoActualTime(String userId) {
        Map<String, Object> result = videoInfoMapper.selectJoinMap(
                new MPJLambdaWrapper<VideoInfo>()
                        .selectSum(VideoInfo::getPlayCount, "playCount")
                        .selectSum(VideoInfo::getLikeCount, "likeCount")
                        .selectSum(VideoInfo::getDanmuCount, "danmuCount")
                        .selectSum(VideoInfo::getCommentCount, "commentCount")
                        .selectSum(VideoInfo::getCoinCount, "coinCount")
                        .selectSum(VideoInfo::getCollectCount, "collectCount")
                        .eq(StrUtil.isNotEmpty(userId), VideoInfo::getUserId, userId)
        );

        // 2. 如果 queryResult 为空（比如没视频），初始化一个空 Map
        if (result == null) {
            result = new HashMap<>();
            // 可以在这里补 0，或者让前端处理 null
        }
        if (!StrUtil.isEmpty(userId)) {
            //查询粉丝数
            result.put("userCount", Math.toIntExact(userFocusMapper.selectCount(new LambdaQueryWrapper<UserFocus>()
                    .eq(UserFocus::getFocusUserId, userId))));
        } else {
            result.put("userCount", Math.toIntExact(userInfoMapper.selectCount(new LambdaQueryWrapper<UserInfo>())));
        }
        return result;
    }

    public List<StatisticsInfo> getPreDayTotalInfo(String preDate) {
        // 使用 MPJLambdaWrapper 进行聚合查询
        List<StatisticsInfo> preDayData = statisticsInfoMapper.selectJoinList(
                StatisticsInfo.class,
                new MPJLambdaWrapper<StatisticsInfo>()
                        // 1. 选取字段并聚合
                        .select(StatisticsInfo::getDataType)
                        .selectSum(StatisticsInfo::getStatisticsCount, StatisticsInfo::getStatisticsCount)
                        // 2. 查询条件
                        .eq(StatisticsInfo::getStatisticsDate, preDate)
                        // 3. 分组逻辑
                        .groupBy(StatisticsInfo::getDataType)
        );

        return preDayData;
    }

}