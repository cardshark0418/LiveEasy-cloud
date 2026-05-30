package com.easylive.controller;

import com.easylive.annotation.GlobalInterceptor;
import com.easylive.annotation.RecordUserMessage;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.enums.MessageTypeEnum;
import com.easylive.redis.RedisComponent;
import com.easylive.service.UserActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController("userActionController")
@RequestMapping("/userAction")
public class UserActionController {

    @Resource
    private UserActionService userActionService;

    @Autowired
    private RedisComponent redisComponent;

    @RequestMapping("doAction")
    @RecordUserMessage(messageType = MessageTypeEnum.LIKE)
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO doAction(@NotEmpty String videoId,
                               @NotNull Integer actionType,
                               @Max(2) @Min(1) Integer actionCount,
                               Integer commentId,
                               HttpServletRequest request) {
        UserAction userAction = new UserAction();
        userAction.setUserId(redisComponent.getTokenUserInfoDto(request).getUserId());
        userAction.setVideoId(videoId);
        userAction.setActionType(actionType);
        actionCount = actionCount == null ? 1 : actionCount;
        userAction.setActionCount(actionCount);
        commentId = commentId == null ? 0 : commentId;
        userAction.setCommentId(commentId);
        userActionService.saveAction(userAction);
        return getSuccessResponseVO(null);
    }

}