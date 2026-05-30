package com.easylive.api.consumer;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;
import java.util.Map;

@FeignClient(name = Constants.SERVER_NAME_WEB, contextId = "userClient")
public interface UserClient {

    @PostMapping(Constants.INNER_API_PREFIX + "/user/getUserInfoBatch")
    Map<String, UserInfo> getUserInfoBatch(@RequestBody Collection<String> userIds);
}