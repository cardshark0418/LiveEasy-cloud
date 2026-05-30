package com.easylive.service;

import com.easylive.entity.po.UserFocus;
import com.github.yulichang.base.MPJBaseService;

import javax.validation.constraints.NotEmpty;

public interface UserFocusService extends MPJBaseService<UserFocus> {

    void focusUser(String userId, @NotEmpty String focusUserId);

    void cancelFocus(String userId, @NotEmpty String focusUserId);
}