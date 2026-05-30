package com.easylive.api.provider;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.easylive.annotation.RecordUserMessage;
import com.easylive.component.EsSearchComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoInfoFile;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.query.VideoInfoPostQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.enums.MessageTypeEnum;
import com.easylive.enums.SearchOrderTypeEnum;
import com.easylive.service.VideoInfoFilePostService;
import com.easylive.service.VideoInfoFileService;
import com.easylive.service.VideoInfoPostService;
import com.easylive.service.VideoInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/video")
@Validated
public class VideoInfoApi {

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private VideoInfoPostService videoInfoPostService;

    @Resource
    private VideoInfoFileService videoInfoFileService;

    @Resource
    private VideoInfoFilePostService videoInfoFilePostService;

    @Resource
    private EsSearchComponent esSearchComponent;

    @RequestMapping("/getVideoInfoFileByVideoId")
    public VideoInfoFile getVideoInfoFile(@NotEmpty String fileId) {
        return videoInfoFileService.getById(fileId);
    }


    @RequestMapping("/getVideoInfoPostByVideoId")
    public VideoInfoPost getVideoInfoPost(@NotEmpty String videoId) {
        return videoInfoPostService.getById(videoId);
    }

    @RequestMapping("/getVideoInfoByVideoId")
    public VideoInfo getVideoInfo(@NotEmpty String videoId) {
        return videoInfoService.getById(videoId);
    }

    @RequestMapping("/updateCountInfo")
    public void updateCountInfo(@NotEmpty String videoId, @NotEmpty String field, @NotNull Integer changeCount) {
//        videoInfoService.updateCountInfo(videoId, field, changeCount);
            videoInfoService.update(null,new LambdaUpdateWrapper<VideoInfo>()
            .setSql(field + "=" + field + "+" + changeCount)
            .eq(VideoInfo::getVideoId,videoId));
    }

    @PostMapping("/getVideoInfoBatch")
    public Map<String, VideoInfo> getVideoInfoBatch(@RequestBody Set<String> videoIds) {
        List<VideoInfo> list = videoInfoService.listByIds(videoIds);
        return list.stream().collect(Collectors.toMap(VideoInfo::getVideoId, Function.identity()));
    }



//    @RequestMapping("/getVideoCount")
//    public Integer getVideoCount(@RequestBody VideoInfoQuery videoInfoQuery) {
//        return videoInfoService.findCountByParam(videoInfoQuery);
//    }

//    @RequestMapping("/getVideoInfoFileByFileId")
//    public VideoInfoFile getVideoInfoFileByFileId(@NotEmpty String fileId) {
//        VideoInfoFile videoInfoFile = videoInfoFileService.getVideoInfoFileByFileId(fileId);
//        return videoInfoFile;
//    }

//    @RequestMapping("/transferVideoFile4Db")
//    public void transferVideoFile4Db(@RequestParam String videoId, @RequestParam String uploadId, @RequestParam String userId,
//                                     @RequestBody VideoInfoFilePost updateFilePost) {
//        videoInfoPostService.transferVideoFile4Db(videoId, uploadId, userId, updateFilePost);
//    }

    @GetMapping("/getVideoIdsByUserId")
    List<String> getVideoIdsByUserId(@RequestParam("userId") String userId){
        LambdaQueryWrapper<VideoInfo> wrapper = new LambdaQueryWrapper<VideoInfo>()
                .eq(VideoInfo::getUserId, userId)
                .select(VideoInfo::getVideoId);
        List<Object> objects = videoInfoService.listObjs(wrapper);

        // 转换为明确的 List<String>，保证 Feign 传输安全
        return objects.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }



    @RequestMapping("/updateDocCount")
    public void updateDocCount(String videoId, SearchOrderTypeEnum searchOrderTypeEnum, Integer changeCount) {
        esSearchComponent.updateDocCount(videoId, searchOrderTypeEnum.getField(), changeCount);
    }

    @RequestMapping("/admin/loadVideoList")
    public PaginationResultVO loadVideoList(@RequestBody VideoInfoPostQuery videoInfoPostQuery) {
        videoInfoPostQuery.setOrderBy("v.last_update_time desc");
        videoInfoPostQuery.setQueryCountInfo(true);
        videoInfoPostQuery.setQueryUserInfo(true);
        PaginationResultVO resultVO = videoInfoPostService.findListByPage(videoInfoPostQuery);
        return resultVO;
    }

    @RequestMapping("/admin/recommendVideo")
    public void recommendVideo(@NotEmpty String videoId) {
        videoInfoPostService.recommendVideo(videoId);
    }

    @RequestMapping("/admin/auditVideo")
    @RecordUserMessage(messageType = MessageTypeEnum.SYS)
    public void auditVideo(@NotEmpty String videoId, @NotNull Integer status, String reason) throws IOException {
        videoInfoPostService.auditVideo(videoId, status, reason);
    }

    @RequestMapping("/admin/deleteVideo")
    public void deleteVideo(@NotEmpty String videoId) {
        videoInfoService.deleteVideo(videoId, null);
    }

    @RequestMapping("/admin/loadVideoPList")
    public List<VideoInfoFilePost> loadVideoPList(@NotEmpty String videoId) {
        return videoInfoFilePostService.list(new LambdaQueryWrapper<VideoInfoFilePost>()
                .eq(VideoInfoFilePost::getVideoId,videoId)
                .orderByAsc(VideoInfoFilePost::getFileIndex));
    }

    @RequestMapping("/getVideoInfoByName")
    List<VideoInfo> getVideoInfoByName(@RequestParam String videoNameFuzzy){
        return videoInfoService.list(new LambdaQueryWrapper<VideoInfo>().like(VideoInfo::getVideoName,videoNameFuzzy));
    }

    @RequestMapping("/getVideoCount")
    Integer getVideoCount(@RequestBody Integer categoryId){
        return Math.toIntExact(videoInfoService.count(new LambdaQueryWrapper<VideoInfo>()
                .eq(VideoInfo::getCategoryId, categoryId)
                .or()
                .eq(VideoInfo::getPCategoryId, categoryId)));
    }
}
