package com.easylive.api.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.StatisticsInfo;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.po.VideoComment;
import com.easylive.entity.po.VideoDanmu;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.enums.UserActionTypeEnum;
import com.easylive.mapper.UserActionMapper;
import com.easylive.mapper.VideoCommentMapper;
import com.easylive.service.UserActionService;
import com.easylive.service.VideoCommentService;
import com.easylive.service.VideoDanmuService;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Validated
@RequestMapping(Constants.INNER_API_PREFIX+"/userAction")
public class UserActionApi {
    @Resource
    private UserActionService userActionService;
    @Resource
    private UserActionMapper userActionMapper;
    @Resource
    private VideoCommentMapper videoCommentMapper;
    @Resource
    private VideoDanmuService videoDanmuService;
    @Resource
    private VideoCommentService videoCommentService;

    @RequestMapping("/getUserActionListWithVideo")
    List<UserAction> getUserActionListWithVideo(@RequestParam String videoId, @RequestParam String userId){
        return userActionService.list(new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getVideoId,videoId)
                .eq(UserAction::getUserId,userId)
                .in(UserAction::getActionType, (Object[]) new Integer[]{UserActionTypeEnum.VIDEO_LIKE.getType(), UserActionTypeEnum.VIDEO_COLLECT.getType(),
                        UserActionTypeEnum.VIDEO_COIN.getType(),}));
    }

    @RequestMapping("/deleteVideoDanmu")
    void deleteVideoDanmu(@RequestParam String videoId){
        videoDanmuService.remove(new LambdaQueryWrapper<VideoDanmu>()
                .eq(VideoDanmu::getVideoId,videoId));
    }

    @RequestMapping("/deleteVideoComment")
    void deleteVideoComment(@RequestParam String videoId){
        videoCommentService.remove(new LambdaQueryWrapper<VideoComment>()
                .eq(VideoComment::getVideoId,videoId));
    }

    @RequestMapping("/selectStatisticsComment")
    List<StatisticsInfo> selectStatisticsComment(@RequestParam String begin, @RequestParam String end){
        return videoCommentMapper.selectJoinList(StatisticsInfo.class,
                new MPJLambdaWrapper<VideoComment>()
                        .selectAs(VideoComment::getVideoUserId, StatisticsInfo::getUserId)
                        .selectCount(VideoComment::getCommentId, StatisticsInfo::getStatisticsCount)
                        .between(VideoComment::getPostTime, begin, end)
                        .groupBy(VideoComment::getVideoUserId));
    }

    @RequestMapping("/selectStatisticsInfo")
    List<StatisticsInfo> selectStatisticsInfo(@RequestParam String begin, @RequestParam String end, @RequestParam Integer[] actionTypes){
        return userActionMapper.selectJoinList(StatisticsInfo.class,
                new MPJLambdaWrapper<UserAction>()
                        .selectAs(UserAction::getVideoUserId, StatisticsInfo::getUserId)
                        .selectAs(UserAction::getActionType, StatisticsInfo::getDataType)
                        .selectCount(UserAction::getVideoUserId, StatisticsInfo::getStatisticsCount)
                        .between(UserAction::getActionTime, begin, end)
                        .in(UserAction::getActionType, (Object[]) actionTypes)
                        .groupBy(UserAction::getVideoUserId)
                        .groupBy(UserAction::getActionType)
        );
    }

    @RequestMapping("/loadUserCollection")
    PaginationResultVO<UserAction> loadUserCollection(@RequestParam String userId, @RequestParam(required = false) Integer pageNo) {
        pageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        Page<UserAction> page = new Page<>(pageNo, 15);
        Page<UserAction> result = userActionMapper.selectPage(page, new LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getUserId, userId)
                .eq(UserAction::getActionType, UserActionTypeEnum.VIDEO_COLLECT.getType())
                .orderByDesc(UserAction::getActionTime));
        return new PaginationResultVO<>(
                (int) result.getTotal(), 15, pageNo, result.getRecords());
    }
}
