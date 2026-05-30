package com.easylive.api.provider;

import com.easylive.entity.constants.Constants;
import com.easylive.enums.MessageTypeEnum;
import com.easylive.service.UserMessageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Validated
@RequestMapping(Constants.INNER_API_PREFIX + "/message")
public class UserMessageApi {
    
    @Resource
    private UserMessageService userMessageService;

    @RequestMapping("/admin/saveUserMessage")
    public void saveUserMessage(@RequestParam String videoId, 
                                @RequestParam(required = false) String userId,
                                @RequestParam Integer messageType,
                                @RequestParam(required = false) String content,
                                @RequestParam(required = false) Integer replyCommentId) {
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(messageType);
        userMessageService.saveUserMessage(videoId, userId, messageTypeEnum, content, replyCommentId);
    }
}
