package com.easylive.api.consumer;

import com.easylive.entity.constants.Constants;
import com.easylive.enums.MessageTypeEnum;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = Constants.SERVER_NAME_INTERACT, contextId = "messageClient")
public interface MessageClient {

    @RequestMapping(Constants.INNER_API_PREFIX + "/message/admin/saveUserMessage")
    void saveUserMessage(@RequestParam String videoId,
                         @RequestParam(required = false) String userId,
                         @RequestParam Integer messageType,
                         @RequestParam(required = false) String content,
                         @RequestParam(required = false) Integer replyCommentId);
}