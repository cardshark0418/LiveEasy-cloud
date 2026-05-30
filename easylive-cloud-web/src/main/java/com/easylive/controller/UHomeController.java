package com.easylive.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easylive.annotation.GlobalInterceptor;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.po.UserFocus;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.UserInfoVO;
import com.easylive.entity.vo.UserLoginDto;
import com.easylive.enums.UserActionTypeEnum;
import com.easylive.enums.VideoOrderTypeEnum;
import com.easylive.redis.RedisComponent;
import com.easylive.service.UserFocusService;
import com.easylive.service.UserInfoService;
import com.easylive.service.VideoInfoService;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@Validated
@RequestMapping("/uhome")
public class UHomeController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private UserFocusService userFocusService;

//    @Resource
//    private UserActionService userActionService;

    @Autowired
    private RedisComponent redisComponent;

    @RequestMapping("/getUserInfo")
//    @GlobalInterceptor
    public ResponseVO getUserInfo(@NotEmpty String userId, HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        UserInfo userInfo = userInfoService.getUserDetailInfo(tokenUserInfoDto == null ? null : tokenUserInfoDto.getUserId(), userId);
        UserInfoVO userInfoVO = BeanUtil.copyProperties(userInfo, UserInfoVO.class);
        return getSuccessResponseVO(userInfoVO);
    }

    @RequestMapping("/updateUserInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO updateUserInfo(@NotEmpty @Size(max = 20) String nickName,
                                     @NotEmpty @Size(max = 100) String avatar,
                                     @NotNull Integer sex, String birthday,
                                     @Size(max = 150) String school,
                                     @Size(max = 80) String personIntroduction,
                                     @Size(max = 300) String noticeInfo,
                                     HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(tokenUserInfoDto.getUserId());
        userInfo.setNickName(nickName);
        userInfo.setAvatar(avatar);
        userInfo.setSex(sex);
        userInfo.setBirthday(birthday);
        userInfo.setSchool(school);
        userInfo.setPersonIntroduction(personIntroduction);
        userInfo.setNoticeInfo(noticeInfo);
        userInfoService.updateUserInfo(userInfo, tokenUserInfoDto);

        return getSuccessResponseVO(null);
    }

    @RequestMapping("/focus")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO focus(@NotEmpty String focusUserId,HttpServletRequest request) {
        userFocusService.focusUser(redisComponent.getTokenUserInfoDto(request).getUserId(), focusUserId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/cancelFocus")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO cancelFocus(@NotEmpty String focusUserId,HttpServletRequest request) {
        userFocusService.cancelFocus(redisComponent.getTokenUserInfoDto(request).getUserId(), focusUserId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadFocusList")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadFocusList(Integer pageNo,HttpServletRequest request) {
        pageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        Page<UserFocus> userFocusPage = userFocusService.selectJoinListPage(new Page<>(pageNo, 15), UserFocus.class,
                new MPJLambdaWrapper<UserFocus>()
                        .selectAll(UserFocus.class)
                        .selectAs(UserInfo::getNickName, UserFocus::getOtherNickName)
                        .selectAs(UserInfo::getUserId, UserFocus::getOtherUserId)
                        .selectAs(UserInfo::getAvatar, UserFocus::getOtherAvatar)
                        .selectAs(UserInfo::getPersonIntroduction, UserFocus::getOtherPersonIntroduction)
                        .leftJoin(UserInfo.class, UserInfo::getUserId, UserFocus::getFocusUserId)
                        .eq(UserFocus::getUserId, tokenUserInfoDto.getUserId())
                        .orderByDesc(UserFocus::getFocusTime));
        PaginationResultVO<UserFocus> resultVO = new PaginationResultVO<UserFocus>((int) userFocusPage.getTotal(),15,pageNo,userFocusPage.getRecords());
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/loadFansList")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadFansList(Integer pageNo,HttpServletRequest request) {
        pageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        Page<UserFocus> userFansPage = userFocusService.selectJoinListPage(new Page<>(pageNo, 15), UserFocus.class,
                new MPJLambdaWrapper<UserFocus>()
                        .selectAll(UserFocus.class)
                        .selectAs(UserInfo::getNickName, UserFocus::getOtherNickName)
                        .selectAs(UserInfo::getUserId, UserFocus::getOtherUserId)
                        .selectAs(UserInfo::getAvatar, UserFocus::getOtherAvatar)
                        .selectAs(UserInfo::getPersonIntroduction, UserFocus::getOtherPersonIntroduction)
                        .leftJoin(UserInfo.class, UserInfo::getUserId, UserFocus::getFocusUserId)
                        .eq(UserFocus::getFocusUserId, tokenUserInfoDto.getUserId())
                        .orderByDesc(UserFocus::getFocusTime));
        PaginationResultVO<UserFocus> resultVO = new PaginationResultVO<UserFocus>((int) userFansPage.getTotal(),15,pageNo,userFansPage.getRecords());
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/loadVideoList")
//    @GlobalInterceptor
    public ResponseVO loadVideoList(@NotEmpty String userId, Integer type, Integer pageNo, String videoName, Integer orderType) {
        pageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        int pageSize = (type==null?15:10);
        VideoOrderTypeEnum videoOrderTypeEnum = VideoOrderTypeEnum.getByType(orderType);
        if (videoOrderTypeEnum == null) {
            videoOrderTypeEnum = VideoOrderTypeEnum.CREATE_TIME;
        }
        MPJLambdaWrapper<VideoInfo> wrapper = new MPJLambdaWrapper<VideoInfo>()
                .eq(VideoInfo::getUserId, userId)
                .orderByDesc(videoOrderTypeEnum.getField());
        if(videoName!=null && !videoName.isEmpty()){
            wrapper.like(VideoInfo::getVideoName, videoName);
        }
        Page<VideoInfo> videoInfoPage = videoInfoService.selectJoinListPage(new Page<>(pageNo, pageSize), VideoInfo.class,wrapper);
        PaginationResultVO<VideoInfo> resultVO = new PaginationResultVO<VideoInfo>((int) videoInfoPage.getTotal(),pageSize,pageNo,videoInfoPage.getRecords());
        return getSuccessResponseVO(resultVO);
    }

//    @RequestMapping("/loadUserCollection")
////    @GlobalInterceptor
//    public ResponseVO loadUserCollection(@NotEmpty String userId, Integer pageNo) {
//        pageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
//
//        Page<UserAction> userActionPage = userActionService.selectJoinListPage(new Page<>(pageNo, 15),
//                UserAction.class,
//                new MPJLambdaWrapper<UserAction>()
//                        .selectAll(UserAction.class)
//                        .select(VideoInfo::getVideoName,VideoInfo::getVideoCover)
//                        .leftJoin(VideoInfo.class,VideoInfo::getVideoId,UserAction::getVideoId)
//                        .eq(UserAction::getUserId,userId)
//                        .eq(UserAction::getActionType,UserActionTypeEnum.VIDEO_COLLECT.getType())
//                        .orderByDesc("action_time"));
//        PaginationResultVO resultVO = new PaginationResultVO((int) userActionPage.getTotal(),15,pageNo,userActionPage.getRecords());
//        return getSuccessResponseVO(resultVO);
//    }
}