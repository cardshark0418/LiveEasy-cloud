package com.easylive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easylive.annotation.GlobalInterceptor;
import com.easylive.api.consumer.UserClient;
import com.easylive.api.consumer.VideoClient;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.po.UserMessage;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.UserLoginDto;
import com.easylive.entity.vo.UserMessageCountDto;
import com.easylive.enums.MessageReadTypeEnum;
import com.easylive.redis.RedisComponent;
import com.easylive.service.UserMessageService;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@Validated
@RequestMapping("/message")
public class UserMessageController{

    @Resource
    private UserMessageService userMessageService;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private UserClient userClient;
    @Resource
    private VideoClient videoClient;

    @RequestMapping("/getNoReadCount")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getNoReadCount(HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        if (tokenUserInfoDto == null) {
            return getSuccessResponseVO(0);
        }
        Integer count = Math.toIntExact(userMessageService.selectJoinCount(new MPJLambdaWrapper<UserMessage>()
                .eq(UserMessage::getUserId,tokenUserInfoDto.getUserId())
                .eq(UserMessage::getReadType, MessageReadTypeEnum.NO_READ.getType())));
        return getSuccessResponseVO(count);
    }

    @RequestMapping("/getNoReadCountGroup")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getNoReadCountGroup(HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        List<UserMessageCountDto> dataList = userMessageService.getMessageTypeNoReadCount(tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(dataList);
    }

    @RequestMapping("/readAll")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO readAll(Integer messageType,HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);

        UserMessage userMessage = new UserMessage();
        userMessage.setReadType(MessageReadTypeEnum.READ.getType());
        userMessageService.update(userMessage, new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getUserId,tokenUserInfoDto.getUserId())
                .eq(messageType != null, UserMessage::getMessageType, messageType));
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadMessage")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadMessage(@NotNull Integer messageType, Integer pageNo,HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        pageNo=pageNo==null?1:pageNo;
//        Page<UserMessage> page = userMessageService.selectJoinListPage(new Page<>(pageNo, 15), UserMessage.class, new MPJLambdaWrapper<UserMessage>()
//                .eq(UserMessage::getMessageType, messageType)
//                .eq(UserMessage::getUserId, tokenUserInfoDto.getUserId())
//                .orderByDesc("message_id")
//                .leftJoin(UserInfo.class,UserInfo::getUserId,UserMessage::getSendUserId)
//                .leftJoin(VideoInfo.class,VideoInfo::getVideoId,UserMessage::getVideoId)
//                .selectAll(UserMessage.class)
//                .selectAs(UserInfo::getNickName,"sendUserName")
//                .selectAs(UserInfo::getAvatar,"sendUserAvatar")
//                .select(VideoInfo::getVideoCover,VideoInfo::getVideoName));
        Page<UserMessage> page = userMessageService.selectJoinListPage(new Page<>(pageNo, 15), UserMessage.class, new MPJLambdaWrapper<UserMessage>()
                .eq(UserMessage::getMessageType, messageType)
                .eq(UserMessage::getUserId, tokenUserInfoDto.getUserId())
                .orderByDesc("message_id"));
        List<UserMessage> list = page.getRecords();
        if (list.isEmpty()) {
            return getSuccessResponseVO(new PaginationResultVO<>((int) page.getTotal(), 15, pageNo, page.getRecords()));
        }

        Set<String> userIdList = list.stream()
                .map(UserMessage::getSendUserId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .collect(Collectors.toSet());
        Set<String> videoIdList = list.stream()
                .map(UserMessage::getVideoId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .collect(Collectors.toSet());

        Map<String, UserInfo> userInfoBatch = userIdList.isEmpty()
                ? new HashMap<>()
                : userClient.getUserInfoBatch(userIdList);
        Map<String, VideoInfo> videoInfoBatch = videoIdList.isEmpty()
                ? new HashMap<>()
                : videoClient.getVideoInfoBatch(videoIdList);
        list.forEach(item -> {
            // 填充用户信息
            if (userInfoBatch != null && userInfoBatch.containsKey(item.getSendUserId())) {
                UserInfo user = userInfoBatch.get(item.getSendUserId());
                item.setSendUserName(user.getNickName());
                item.setSendUserAvatar(user.getAvatar());
            }

            // 填充视频信息
            if (videoInfoBatch != null && videoInfoBatch.containsKey(item.getVideoId())) {
                VideoInfo video = videoInfoBatch.get(item.getVideoId());
                item.setVideoCover(video.getVideoCover());
                item.setVideoName(video.getVideoName());
            }
        });
        if (!list.isEmpty()) {
            // 提取当前页的消息ID
            List<Integer> messageIds = list.stream()
                    .map(UserMessage::getMessageId)
                    .collect(Collectors.toList());

            userMessageService.update(new LambdaUpdateWrapper<UserMessage>()
                    .set(UserMessage::getReadType, MessageReadTypeEnum.READ.getType())
                    .in(UserMessage::getMessageId, messageIds)
                    .eq(UserMessage::getUserId, tokenUserInfoDto.getUserId()) // 安全起见检查UserId
            );
        }

        return getSuccessResponseVO(new PaginationResultVO<>((int) page.getTotal(),15,pageNo,page.getRecords()));
    }


    @RequestMapping("/delMessage")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delMessage(@NotNull Integer messageId, HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        userMessageService.remove(new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getUserId,tokenUserInfoDto.getUserId())
                .eq(UserMessage::getMessageId,messageId));
        return getSuccessResponseVO(null);
    }
}
