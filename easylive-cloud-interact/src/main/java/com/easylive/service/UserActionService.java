package com.easylive.service;

import com.easylive.entity.po.UserAction;
import com.github.yulichang.base.MPJBaseService;

public interface UserActionService extends MPJBaseService<UserAction> {
    void saveAction(UserAction userAction);
}