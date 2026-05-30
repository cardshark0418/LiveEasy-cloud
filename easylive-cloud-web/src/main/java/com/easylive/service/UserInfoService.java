package com.easylive.service;

import com.easylive.entity.po.UserInfo;
import com.easylive.entity.vo.UserLoginDto;
import com.github.yulichang.base.MPJBaseService;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.Map;

public interface UserInfoService extends MPJBaseService<UserInfo> {
    void register(String email, String nickName, String password);

    UserLoginDto login(@NotEmpty @Email String email, @NotEmpty String password, String ip, HttpServletResponse response);

    UserInfo getUserDetailInfo(String s, @NotEmpty String userId);

    void updateUserInfo(UserInfo userInfo, UserLoginDto tokenUserInfoDto);

    Map<String, UserInfo> getUserInfoBatch(Collection<String> userIds);
}
