package com.easylive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easylive.api.consumer.UserActionClient;
import com.easylive.component.EsSearchComponent;
import com.easylive.enums.ResponseCodeEnum;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoInfoFile;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.UserLoginDto;
import com.easylive.entity.vo.VideoInfoResultVo;
import com.easylive.enums.SearchOrderTypeEnum;
import com.easylive.enums.VideoRecommendTypeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.redis.RedisComponent;
import com.easylive.redis.RedisUtils;
import com.easylive.service.VideoInfoFileService;
import com.easylive.service.VideoInfoService;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@Validated
@RequestMapping("/video")
@Slf4j
public class VideoController {

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private VideoInfoFileService videoInfoFileService;


    @Resource
    private RedisComponent redisComponent;

    @Autowired
    private RedisUtils redisUtils;

    @Resource
    private UserActionClient interactClient;

    @Resource
    private EsSearchComponent esSearchComponent;


    @RequestMapping("/loadRecommendVideo")
//    @GlobalInterceptor
    public ResponseVO loadRecommendVideo() {
        List<VideoInfo> recommendVideoList = videoInfoService.list(new MPJLambdaWrapper<VideoInfo>()
                .selectAll(VideoInfo.class) // 1. 查出 VideoInfo 表所有字段
                .select(UserInfo::getNickName, UserInfo::getAvatar) // 2. 查出 UserInfo 里的字段
                .eq(VideoInfo::getRecommendType, VideoRecommendTypeEnum.RECOMMEND.getType())
                .orderByDesc(VideoInfo::getCreateTime)
                .leftJoin(UserInfo.class,UserInfo::getUserId, VideoInfo::getUserId));
        return getSuccessResponseVO(recommendVideoList);
    }

    @RequestMapping("/loadVideo")
//    @GlobalInterceptor
    public ResponseVO loadVideo(Integer pCategoryId, Integer categoryId, Integer pageNo) {
        MPJLambdaWrapper<VideoInfo> wrapper = new MPJLambdaWrapper<VideoInfo>()
                .selectAll(VideoInfo.class)
                .select(UserInfo::getNickName, UserInfo::getAvatar)
                .leftJoin(UserInfo.class, UserInfo::getUserId, VideoInfo::getUserId)
                .orderByDesc(VideoInfo::getCreateTime)
                .eq(categoryId != null,VideoInfo::getCategoryId, categoryId)
                .eq(pCategoryId != null,VideoInfo::getPCategoryId, pCategoryId);
        if (categoryId == null && pCategoryId == null){
            wrapper.eq(VideoInfo::getRecommendType,VideoRecommendTypeEnum.NO_RECOMMEND.getType());
        }
        IPage<VideoInfo> iPage = videoInfoService.selectJoinListPage(new Page<>(pageNo == null ? 1 : pageNo, 15),
                VideoInfo.class,wrapper);
        PaginationResultVO resultVO = new PaginationResultVO<>((int)iPage.getTotal(),(int)iPage.getSize(),(int)iPage.getCurrent(),iPage.getRecords());
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/getVideoInfo")
//    @GlobalInterceptor
    public ResponseVO getVideoInfo(@NotEmpty String videoId, HttpServletRequest request) {
        VideoInfo videoInfo = videoInfoService.getById(videoId);
        if (null == videoInfo) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        UserLoginDto userInfoDto = redisComponent.getTokenUserInfoDto(request);

        List<UserAction> userActionList = new ArrayList<>();
        if (userInfoDto != null) {
            userActionList = interactClient.getUserActionListWithVideo(videoId,userInfoDto.getUserId());
        }
        VideoInfoResultVo resultVo = new VideoInfoResultVo();
        resultVo.setVideoInfo(videoInfo);
        resultVo.setUserActionList(userActionList);
        return getSuccessResponseVO(resultVo);
    }

    @RequestMapping("/loadVideoPList")
//    @GlobalInterceptor
    public ResponseVO loadVideoPList(@NotEmpty String videoId) {
        List<VideoInfoFile> fileList = videoInfoFileService.list(new LambdaQueryWrapper<VideoInfoFile>()
                .eq(VideoInfoFile::getVideoId, videoId)
                .orderByAsc(VideoInfoFile::getFileIndex));
        return getSuccessResponseVO(fileList);
    }

    @RequestMapping("/reportVideoPlayOnline")
//    @GlobalInterceptor
    public ResponseVO reportVideoPlayOnline(@NotEmpty String fileId, String deviceId) {
        Integer count = redisComponent.reportVideoPlayOnline(fileId, deviceId);
        return getSuccessResponseVO(count);
    }

    @RequestMapping("/search")
//    @GlobalInterceptor
    public ResponseVO search(@NotEmpty String keyword, Integer orderType, Integer pageNo) {
        redisComponent.addKeywordCount(keyword);
        PaginationResultVO resultVO = esSearchComponent.search(true, keyword, orderType, pageNo, 30);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/getVideoRecommend")
//    @GlobalInterceptor
    public ResponseVO getVideoRecommend(@NotEmpty String keyword, @NotEmpty String videoId) {
        List<VideoInfo> videoInfoList = esSearchComponent.search(false, keyword, SearchOrderTypeEnum.VIDEO_PLAY.getType(), 1, 10).getList();
        videoInfoList = videoInfoList.stream().filter(item -> !item.getVideoId().equals(videoId)).collect(Collectors.toList());
        return getSuccessResponseVO(videoInfoList);
    }

    @RequestMapping("/getSearchKeywordTop")
//    @GlobalInterceptor
    public ResponseVO getSearchKeywordTop() {
        List<Object> keywordList = redisComponent.getKeywordTop(10);
        return getSuccessResponseVO(keywordList);
    }

    @RequestMapping("/loadHotVideoList")
//    @GlobalInterceptor
    public ResponseVO loadHotVideoList(Integer pageNo) {
        pageNo=(pageNo==null || pageNo<=0)?1:pageNo;
        Page<VideoInfo> videoInfoPage = videoInfoService.selectJoinListPage(new Page<>(pageNo, 15), VideoInfo.class, new MPJLambdaWrapper<VideoInfo>()
                .orderByDesc("play_count")
                .leftJoin(UserInfo.class, UserInfo::getUserId, VideoInfo::getUserId)
                .selectAll(VideoInfo.class)
                .select(UserInfo::getNickName, UserInfo::getAvatar)
                .gt(VideoInfo::getCreateTime, LocalDateTime.now().minusHours(24)));
        return getSuccessResponseVO(new PaginationResultVO<>((int) videoInfoPage.getTotal(),15,pageNo,videoInfoPage.getRecords()));
    }
}