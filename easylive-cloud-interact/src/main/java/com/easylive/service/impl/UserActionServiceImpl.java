package com.easylive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import com.easylive.api.consumer.VideoClient;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.po.VideoComment;
import com.easylive.entity.po.VideoInfo;
import com.easylive.enums.ResponseCodeEnum;
import com.easylive.enums.SearchOrderTypeEnum;
import com.easylive.enums.UserActionTypeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.mapper.UserActionMapper;

import com.easylive.mapper.VideoCommentMapper;

import com.easylive.service.UserActionService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserActionServiceImpl extends MPJBaseServiceImpl<UserActionMapper, UserAction> implements UserActionService {

    @Autowired
    private UserActionMapper userActionMapper;
    @Autowired
    private VideoClient videoClient;
    @Autowired
    private VideoCommentMapper videoCommentMapper;

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void saveAction(UserAction bean) {
        VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(bean.getVideoId());
        if (videoInfo == null) {//视频不存在 报错
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        bean.setVideoUserId(videoInfo.getUserId());

        UserActionTypeEnum actionTypeEnum = UserActionTypeEnum.getByType(bean.getActionType());
        if (actionTypeEnum == null) {//互动类型不存在 报错
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

//        UserAction dbAction = userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(bean.getVideoId(), bean.getCommentId(), bean.getActionType(),
//                bean.getUserId());
        UserAction dbAction = userActionMapper.selectOne(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getVideoId, bean.getVideoId())
                .eq(UserAction::getActionType, bean.getActionType())
                .eq(UserAction::getCommentId, bean.getCommentId())
                .eq(UserAction::getUserId, bean.getUserId()));

        bean.setActionTime(new Date());
        switch (actionTypeEnum) {
            //点赞,收藏
            case VIDEO_LIKE:
            case VIDEO_COLLECT:
                if (dbAction != null) {
                    userActionMapper.deleteById(dbAction.getActionId());
                } else {
                    userActionMapper.insert(bean);
                }
                Integer changeCount = bean.getActionCount();
//                videoInfoMapper.update(null,new LambdaUpdateWrapper<VideoInfo>()
//                        .setSql(actionTypeEnum.getField() + "=" + actionTypeEnum.getField() + "+" + changeCount)
//                        .eq(VideoInfo::getVideoId,bean.getVideoId()));
                videoClient.updateCountInfo(bean.getVideoId(),actionTypeEnum.getField(),changeCount);

                if (actionTypeEnum == UserActionTypeEnum.VIDEO_COLLECT) {
                    //更新es收藏数量
                    videoClient.updateDocCount(videoInfo.getVideoId(), SearchOrderTypeEnum.VIDEO_COLLECT, changeCount);
                }
                break;
            case VIDEO_COIN:
                if (videoInfo.getUserId().equals(bean.getUserId())) {
                    throw new BusinessException("UP主不能给自己投币");
                }
                if (dbAction != null) {
                    throw new BusinessException("对本稿件的投币枚数已用完");
                }
                //减少自己的硬币
//                int updateCount = userInfoMapper.update(null, new LambdaUpdateWrapper<UserInfo>()
//                        .ge(UserInfo::getTotalCoinCount, bean.getActionCount())
//                        .eq(UserInfo::getUserId, bean.getUserId())
//                        .setSql("total_coin_count = total_coin_count - " + bean.getActionCount()));
                Integer updateCount = videoClient.updateCoinCountInfo(bean.getUserId(), -bean.getActionCount());
                if (updateCount == 0) {
                    throw new BusinessException("币不够");
                }
                //增加up主的硬币
//                updateCount = userInfoMapper.update(null, new LambdaUpdateWrapper<UserInfo>()
//                        .eq(UserInfo::getUserId, videoInfo.getUserId())
//                        .setSql("total_coin_count = total_coin_count + " + bean.getActionCount()));
                updateCount = videoClient.updateCoinCountInfo(bean.getUserId(), bean.getActionCount());
                if (updateCount == 0) {
                    throw new BusinessException("投币失败");
                }
                userActionMapper.insert(bean);
                videoClient.updateCountInfo(bean.getVideoId(),actionTypeEnum.getField(),bean.getActionCount());
//                videoInfoMapper.update(null,new LambdaUpdateWrapper<VideoInfo>()
//                        .setSql(actionTypeEnum.getField() + "=" + actionTypeEnum.getField() + "+" + bean.getActionCount())
//                        .eq(VideoInfo::getVideoId,bean.getVideoId()));
                break;
            //评论
            case COMMENT_LIKE:
            case COMMENT_HATE:
                UserActionTypeEnum opposeTypeEnum = UserActionTypeEnum.COMMENT_LIKE == actionTypeEnum ? UserActionTypeEnum.COMMENT_HATE : UserActionTypeEnum.COMMENT_LIKE;
//                UserAction opposeAction = userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(bean.getVideoId(), bean.getCommentId(),
//                        opposeTypeEnum.getType(), bean.getUserId());
                UserAction opposeAction = userActionMapper.selectOne(new LambdaQueryWrapper<UserAction>()
                        .eq(UserAction::getUserId, bean.getUserId())
                        .eq(UserAction::getActionType, opposeTypeEnum.getType())
                        .eq(UserAction::getCommentId, bean.getCommentId())
                        .eq(UserAction::getVideoId, bean.getVideoId()));
                if (opposeAction != null) {
                    userActionMapper.deleteById(opposeAction.getActionId());
                }

                if (dbAction != null) {
                    userActionMapper.deleteById(dbAction.getActionId());
                } else {
                    userActionMapper.insert(bean);
                }
                changeCount = dbAction == null ? 1 : -1;
                int opposeChangeCount = changeCount.equals(1)?-1:1;
                videoCommentMapper.update(null,new LambdaUpdateWrapper<VideoComment>()
                        .eq(VideoComment::getCommentId,bean.getCommentId())
                        .setSql(actionTypeEnum.getField() + "=" + actionTypeEnum.getField() + "+" + changeCount));
                if(opposeAction!=null && dbAction==null){
                    videoCommentMapper.update(null,new LambdaUpdateWrapper<VideoComment>()
                            .eq(VideoComment::getCommentId,bean.getCommentId())
                            .setSql(opposeTypeEnum.getField() + "=" + opposeTypeEnum.getField() + "+" + opposeChangeCount));
                }
                break;
        }
    }
}