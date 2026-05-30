package com.easylive.service;

import com.easylive.entity.po.UserMessage;
import com.easylive.entity.vo.UserMessageCountDto;
import com.easylive.enums.MessageTypeEnum;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

public interface UserMessageService extends MPJBaseService<UserMessage> {
    void saveUserMessage(String videoId, String s, MessageTypeEnum messageTypeEnum, String content, Integer replyCommentId);

    List<UserMessageCountDto> getMessageTypeNoReadCount(String userId);
}