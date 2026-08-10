package com.easylive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.annotation.GlobalInterceptor;
import com.easylive.annotation.RecordUserMessage;
import com.easylive.api.consumer.UserClient;
import com.easylive.api.consumer.VideoClient;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.po.VideoComment;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.UserLoginDto;
import com.easylive.entity.vo.VideoCommentResultVO;
import com.easylive.enums.MessageTypeEnum;
import com.easylive.enums.UserActionTypeEnum;
import com.easylive.redis.RedisComponent;
import com.easylive.service.UserActionService;
import com.easylive.service.VideoCommentService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@Validated
@RequestMapping("/comment")
@Slf4j
public class VideoCommentController{

    @Resource
    private VideoCommentService videoCommentService;

    @Resource
    private UserActionService userActionService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private VideoClient videoClient;

    @Resource
    private UserClient userClient;

    @RequestMapping("/loadComment")
//    @GlobalInterceptor
    public ResponseVO loadComment(
            @NotEmpty String videoId,
            @RequestParam(defaultValue = "1") String pageNo,
            @RequestParam(defaultValue = "0") Integer orderType,
            HttpServletRequest request) {
        Integer pageNoInt = 0;
        try {
            pageNoInt = Integer.parseInt(pageNo);
        } catch (NumberFormatException e) {
            pageNoInt = 1;
        }
        VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(videoId);
        if (videoInfo == null) {
            return getSuccessResponseVO(new VideoCommentResultVO());
        }
        // "1"=关闭评论
        if (isInteractionFlagOn(videoInfo.getInteraction(), "1")) {
            return getSuccessResponseVO(new VideoCommentResultVO());
        }

        PaginationResultVO<VideoComment> commentData = videoCommentService.getCommentList(videoId, pageNoInt, orderType);

        List<VideoComment> rootList = commentData.getList();

        if (pageNo == null || pageNoInt == 1) {
            VideoComment topComment = videoCommentService.getOne(new LambdaQueryWrapper<VideoComment>()
                    .eq(VideoComment::getVideoId, videoId)
                    .eq(VideoComment::getTopType, 1)
                    .last("LIMIT 1"));

            if (topComment != null) {
                rootList = rootList.stream()
                        .filter(item -> !item.getCommentId().equals(topComment.getCommentId()))
                        .collect(Collectors.toList());
                rootList.add(0, topComment);
            }
        }

        if (!rootList.isEmpty()) {
            List<Integer> parentIds = rootList.stream()
                    .map(VideoComment::getCommentId)
                    .collect(Collectors.toList());

            List<VideoComment> allChildren = videoCommentService.list(new LambdaQueryWrapper<VideoComment>()
                    .in(VideoComment::getPCommentId, parentIds)
                    .orderByAsc(VideoComment::getPostTime));

            Map<Integer, List<VideoComment>> childrenMap = allChildren.stream()
                    .collect(Collectors.groupingBy(VideoComment::getPCommentId));

            rootList.forEach(parent -> {
                parent.setChildren(childrenMap.getOrDefault(parent.getCommentId(), new ArrayList<>()));
            });
        }

        commentData.setList(rootList);

        Set<String> userIds = new java.util.HashSet<>();
        for (VideoComment comment : rootList) {
            if (comment.getUserId() != null) {
                userIds.add(comment.getUserId());
            }
            if (comment.getReplyUserId() != null) {
                userIds.add(comment.getReplyUserId());
            }
            if (comment.getChildren() != null) {
                for (VideoComment child : comment.getChildren()) {
                    if (child.getUserId() != null) {
                        userIds.add(child.getUserId());
                    }
                    if (child.getReplyUserId() != null) {
                        userIds.add(child.getReplyUserId());
                    }
                }
            }
        }

        if (!userIds.isEmpty()) {
            Map<String, UserInfo> userInfoMap = userClient.getUserInfoBatch(userIds);
            
            for (VideoComment comment : rootList) {
                fillUserInfo(comment, userInfoMap);
                if (comment.getChildren() != null) {
                    for (VideoComment child : comment.getChildren()) {
                        fillUserInfo(child, userInfoMap);
                    }
                }
            }
        }

        VideoCommentResultVO resultVO = new VideoCommentResultVO();
        resultVO.setCommentData(commentData);
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        List<UserAction> userActionList = new ArrayList<>();
        if (tokenUserInfoDto != null) {
            userActionList = userActionService.list(new LambdaQueryWrapper<UserAction>()
                    .eq(UserAction::getUserId, tokenUserInfoDto.getUserId())
                    .eq(UserAction::getVideoId, videoId)
                    .in(UserAction::getActionType, (Object[]) new Integer[]{UserActionTypeEnum.COMMENT_LIKE.getType(), UserActionTypeEnum.COMMENT_HATE.getType()}));
        }
        resultVO.setUserActionList(userActionList);
        return getSuccessResponseVO(resultVO);
    }

    private boolean isInteractionFlagOn(String interaction, String flag) {
        if (interaction == null || interaction.isEmpty()) {
            return false;
        }
        for (String part : interaction.split(",")) {
            if (flag.equals(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private void fillUserInfo(VideoComment comment, Map<String, UserInfo> userInfoMap) {
        if (comment.getUserId() != null && userInfoMap.containsKey(comment.getUserId())) {
            UserInfo userInfo = userInfoMap.get(comment.getUserId());
            comment.setNickName(userInfo.getNickName());
            comment.setAvatar(userInfo.getAvatar());
        }
        
        if (comment.getReplyUserId() != null && userInfoMap.containsKey(comment.getReplyUserId())) {
            UserInfo replyUserInfo = userInfoMap.get(comment.getReplyUserId());
            comment.setReplyNickName(replyUserInfo.getNickName());
            comment.setReplyAvatar(replyUserInfo.getAvatar());
        }
    }


    @RequestMapping("/postComment")
    @GlobalInterceptor(checkLogin = true)
    @RecordUserMessage(messageType = MessageTypeEnum.COMMENT)
    @GlobalTransactional(rollbackFor = Exception.class)
    public ResponseVO postComment(@NotEmpty String videoId,
                                  Integer replyCommentId,
                                  @NotEmpty @Size(max = 500) String content,
                                  @Size(max = 50) String imgPath,
                                  HttpServletRequest request) {

        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        VideoComment comment = new VideoComment();
        comment.setUserId(tokenUserInfoDto.getUserId());
        comment.setAvatar(tokenUserInfoDto.getAvatar());
        comment.setNickName(tokenUserInfoDto.getNickName());
        comment.setVideoId(videoId);
        comment.setContent(content);
        comment.setImgPath(imgPath);
        videoCommentService.postComment(comment, replyCommentId);
        return getSuccessResponseVO(comment);
    }


    @RequestMapping("/userDelComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO userDelComment(@NotNull Integer commentId,HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        VideoComment comment = new VideoComment();
        videoCommentService.deleteComment(commentId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(comment);
    }

    @RequestMapping("/topComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO topComment(@NotNull Integer commentId, HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        videoCommentService.topComment(commentId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/cancelTopComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO cancelTopComment(@NotNull Integer commentId, HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        videoCommentService.cancelTopComment(commentId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }


    @RequestMapping("/delComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delComment(@NotNull Integer commentId, HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        videoCommentService.deleteComment(commentId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }

}