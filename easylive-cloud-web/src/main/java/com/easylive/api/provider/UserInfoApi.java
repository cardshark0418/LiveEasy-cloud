package com.easylive.api.provider;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.query.UserInfoQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.redis.RedisUtils;
import com.easylive.service.UserInfoService;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@Validated
@RequestMapping(Constants.INNER_API_PREFIX + "/user")
public class UserInfoApi {


    @Resource
    private UserInfoService userInfoService;

    @Resource
    private RedisUtils redisUtils;

    @RequestMapping("/updateCoinCountInfo")
    public Integer updateCoinCountInfo(@NotEmpty String userId, @NotNull Integer count) {

        boolean updateCount = userInfoService.update(null, new LambdaUpdateWrapper<UserInfo>()
        .ge(UserInfo::getTotalCoinCount, count)
        .eq(UserInfo::getUserId, userId)
        .setSql("total_coin_count = total_coin_count - " + count));
        return updateCount?1:0;
    }


    @RequestMapping("/getUserInfoByUserId")
    public UserInfo getUserInfoByUserId(@NotEmpty String userId) {
        return userInfoService.getById(userId);
    }



    @PostMapping("/getUserInfoBatch")
    public Map<String, UserInfo> getUserInfoBatch(@RequestBody Collection<String> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return new HashMap<>();
        }
        // 调用 Service 处理（带缓存逻辑）
        return userInfoService.getUserInfoBatch(userIds);
    }

    @RequestMapping("/loadUser")
    public PaginationResultVO loadUser(@RequestParam(required = false) Integer pageNo,@RequestParam(required = false) String nickNameFuzzy,@RequestParam(required = false) Integer status) {
        pageNo= pageNo==null?1:pageNo;
        Page<UserInfo> page = userInfoService.selectJoinListPage(new Page<UserInfo>(pageNo, 15), UserInfo.class, new MPJLambdaWrapper<UserInfo>()
                .orderByDesc(UserInfo::getJoinTime)
                .eq(status != null, UserInfo::getStatus, status)
                .like(!StrUtil.isBlank(nickNameFuzzy), UserInfo::getNickName, nickNameFuzzy));
        return new PaginationResultVO<>((int) page.getTotal(),15,pageNo,page.getRecords());
    }


    @RequestMapping("/changeStatus")
    public void changeStatus(@RequestParam String userId, @RequestParam Integer status) {
        UserInfo userInfo = new UserInfo();
        userInfo.setStatus(status);
        userInfo.setUserId(userId);
        userInfoService.updateById(userInfo);
        if(status==0){
            String token = (String) redisUtils.get(Constants.REDIS_KEY_USER_TOKEN + userId);
            if(StrUtil.isEmpty(token)) return;
            redisUtils.delete(Constants.REDIS_KEY_USER_TOKEN + userId);
            redisUtils.delete(Constants.REDIS_KEY_LOGIN_TOKEN + token);
        }
    }

}