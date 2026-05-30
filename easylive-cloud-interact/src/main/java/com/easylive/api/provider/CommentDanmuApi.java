package com.easylive.api.provider;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easylive.api.consumer.VideoClient;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.*;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.mapper.VideoCommentMapper;
import com.easylive.service.VideoCommentService;
import com.easylive.service.VideoDanmuService;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping(Constants.INNER_API_PREFIX)
public class CommentDanmuApi {

    @Resource
    private VideoCommentMapper videoCommentMapper;
    @Resource
    private VideoDanmuService videoDanmuService;
    @Resource
    private VideoCommentService videoCommentService;
    @Resource
    private VideoClient videoClient;


    @RequestMapping("/danmu/admin/loadDanmu")
    PaginationResultVO loadDanmu(@RequestParam(required = false) Integer pageNo, @RequestParam(required = false) String videoNameFuzzy){
        pageNo= pageNo==null?1:pageNo;

        if(StrUtil.isBlank(videoNameFuzzy)) return new PaginationResultVO<>();
        List<VideoInfo> videoInfoList = videoClient.getVideoInfoByName(videoNameFuzzy);
        if (CollUtil.isEmpty(videoInfoList)) {
            return new PaginationResultVO<>(0, 15, pageNo, new ArrayList<>());
        }

        Map<String, String> videoNameMap = videoInfoList.stream()
                .collect(Collectors.toMap(VideoInfo::getVideoId, VideoInfo::getVideoName));
        List<String> videoIds = new ArrayList<>(videoNameMap.keySet());

        Page<VideoDanmu> page = videoDanmuService.selectJoinListPage(
                new Page<>(pageNo, 15),
                VideoDanmu.class,
                new MPJLambdaWrapper<VideoDanmu>()
                        .orderByDesc(VideoDanmu::getDanmuId)
                        .in(VideoDanmu::getVideoId, videoIds)
        );

        page.getRecords().forEach(danmu -> {
            danmu.setVideoName(videoNameMap.get(danmu.getVideoId()));
        });

        return new PaginationResultVO<>((int) page.getTotal(), 15, pageNo, page.getRecords());
    }

    @RequestMapping("/danmu/admin/delDanmu")
    void delDanmu(@RequestParam Integer danmuId){
        videoDanmuService.deleteDanmu(null, danmuId);
    }

    @RequestMapping("/comment/admin/loadComment")
    PaginationResultVO loadComment(@RequestParam(required = false) Integer pageNo, @RequestParam(required = false) String videoNameFuzzy){
        pageNo= pageNo==null?1:pageNo;

        if(StrUtil.isBlank(videoNameFuzzy)) return new PaginationResultVO<>();
        List<VideoInfo> videoInfoList = videoClient.getVideoInfoByName(videoNameFuzzy);
        if (CollUtil.isEmpty(videoInfoList)) {
            return new PaginationResultVO<>(0, 15, pageNo, new ArrayList<>());
        }

        Map<String, String> videoNameMap = videoInfoList.stream()
                .collect(Collectors.toMap(VideoInfo::getVideoId, VideoInfo::getVideoName));
        List<String> videoIds = new ArrayList<>(videoNameMap.keySet());

        Page<VideoComment> page = videoCommentService.selectJoinListPage(
                new Page<>(pageNo, 15),
                VideoComment.class,
                new MPJLambdaWrapper<VideoComment>()
                        .orderByDesc(VideoComment::getCommentId)
                        .in(VideoComment::getVideoId, videoIds)
        );

        page.getRecords().forEach(comment -> {
            comment.setVideoName(videoNameMap.get(comment.getVideoId()));
        });

        return new PaginationResultVO<>((int) page.getTotal(), 15, pageNo, page.getRecords());
    }

    @RequestMapping("/comment/admin/delComment")
    void delComment(@RequestParam Integer commentId){
        videoCommentService.deleteComment(commentId, null);
    }



}