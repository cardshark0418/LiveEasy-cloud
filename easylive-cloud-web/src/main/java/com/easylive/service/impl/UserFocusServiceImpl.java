package com.easylive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.entity.po.UserFocus;
import com.easylive.entity.po.UserInfo;
import com.easylive.enums.ResponseCodeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.mapper.UserFocusMapper;
import com.easylive.mapper.UserInfoMapper;
import com.easylive.service.UserFocusService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserFocusServiceImpl extends MPJBaseServiceImpl<UserFocusMapper, UserFocus> implements UserFocusService {

    @Autowired
    private UserFocusMapper userFocusMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Override
    public void focusUser(String userId, String focusUserId) {
        if (userId.equals(focusUserId)) {
            throw new BusinessException("不能对自己进行此操作");
        }
        UserFocus dbInfo = this.userFocusMapper.selectOne(new LambdaQueryWrapper<UserFocus>()
                .eq(UserFocus::getUserId,userId)
                .eq(UserFocus::getFocusUserId,focusUserId));
        if (dbInfo != null) {
            return;
        }
        UserInfo userInfo = userInfoMapper.selectById(focusUserId);
        if (userInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserFocus focus = new UserFocus();
        focus.setUserId(userId);
        focus.setFocusUserId(focusUserId);
        focus.setFocusTime(new Date());
        this.userFocusMapper.insert(focus);
    }

    @Override
    public void cancelFocus(String userId, String focusUserId) {
        userFocusMapper.delete(new LambdaQueryWrapper<UserFocus>()
                .eq(UserFocus::getFocusUserId,focusUserId)
                .eq(UserFocus::getUserId,userId));
    }
}