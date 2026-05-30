package com.easylive.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.api.consumer.VideoClient;
import com.easylive.entity.po.UserMessage;
import com.easylive.entity.po.VideoComment;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.vo.UserMessageCountDto;
import com.easylive.entity.vo.UserMessageExtendDto;
import com.easylive.enums.MessageReadTypeEnum;
import com.easylive.enums.MessageTypeEnum;
import com.easylive.mapper.UserMessageMapper;
import com.easylive.mapper.VideoCommentMapper;
import com.easylive.service.UserMessageService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class UserMessageServiceImpl extends MPJBaseServiceImpl<UserMessageMapper, UserMessage> implements UserMessageService {
    @Resource
    private UserMessageMapper userMessageMapper;

    @Resource
    private VideoClient videoClient;

    @Resource
    private VideoCommentMapper videoCommentMapper;
    @Override
    @Async
    public void saveUserMessage(String videoId, String sendUserId, MessageTypeEnum messageTypeEnum, String content, Integer replyCommentId) {
        VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(videoId);
        if (videoInfo == null) {
            return;
        }

        UserMessageExtendDto extendDto = new UserMessageExtendDto();
        extendDto.setMessageContent(content);

        String userId = videoInfo.getUserId();

        //收藏，点赞 如果已经记录过消息（点赞过又取消了 再点），不在记录
        if (ArrayUtils.contains(new Integer[]{MessageTypeEnum.LIKE.getType(), MessageTypeEnum.COLLECTION.getType()}, messageTypeEnum.getType())) {
            Integer count = Math.toIntExact(userMessageMapper.selectCount(new LambdaQueryWrapper<UserMessage>()
                    .eq(UserMessage::getSendUserId, sendUserId)
                    .eq(UserMessage::getVideoId,videoId)
                    .eq(UserMessage::getMessageType,messageTypeEnum.getType())));
            if (count > 0) {
                return;
            }
        }
        UserMessage userMessage = new UserMessage();
        userMessage.setUserId(userId);
        userMessage.setVideoId(videoId);
        userMessage.setReadType(MessageReadTypeEnum.NO_READ.getType());
        userMessage.setCreateTime(new Date());
        userMessage.setMessageType(messageTypeEnum.getType());
        userMessage.setSendUserId(sendUserId);

        //评论特殊处理
        if (replyCommentId != null) {
            VideoComment commentInfo = videoCommentMapper.selectById(replyCommentId);
            if (null != commentInfo) {
                userId = commentInfo.getUserId();
                extendDto.setMessageContentReply(commentInfo.getContent());
            }
        }
        if (userId.equals(sendUserId)) {
            return;
        }

        //系统消息特殊处理
        if (MessageTypeEnum.SYS == messageTypeEnum) {
//            VideoInfoPost videoInfoPost = videoInfoPostMapper.selectById(videoId);
            VideoInfoPost videoInfoPost = videoClient.getVideoInfoPostByVideoId(videoId);
            extendDto.setAuditStatus(videoInfoPost.getStatus());
        }
        userMessage.setUserId(userId);
        userMessage.setExtendJson(JSONUtil.toJsonStr(extendDto));
        this.userMessageMapper.insert(userMessage);
    }

    @Override
    public List<UserMessageCountDto> getMessageTypeNoReadCount(String userId) {
        return userMessageMapper.getNoReadCountGroup(userId);
    }


}