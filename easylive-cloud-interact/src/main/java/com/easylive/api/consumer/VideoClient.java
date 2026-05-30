package com.easylive.api.consumer;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoInfoPost;
import com.easylive.enums.SearchOrderTypeEnum;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FeignClient(name = Constants.SERVER_NAME_WEB, contextId = "videoClient")
public interface  VideoClient {

    @GetMapping(Constants.INNER_API_PREFIX + "/user/updateCoinCountInfo")
    Integer updateCoinCountInfo(@RequestParam String userId, @RequestParam Integer count);

    @GetMapping(Constants.INNER_API_PREFIX + "/user/getUserInfoByUserId")
    UserInfo getUserInfoByUserId(@RequestParam String userId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/getVideoInfoByVideoId")
    VideoInfo getVideoInfoByVideoId(@RequestParam String videoId);

    //                updateCount = userInfoMapper.update(null, new LambdaUpdateWrapper<UserInfo>()
    //                        .eq(UserInfo::getUserId, videoInfo.getUserId())
    //                        .setSql("total_coin_count = total_coin_count + " + bean.getActionCount()));
    @RequestMapping(Constants.INNER_API_PREFIX + "/video/updateCountInfo")
    Integer updateCountInfo(@RequestParam String videoId, @RequestParam String field,
                            @RequestParam Integer changeCount);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/getVideoInfoPostByVideoId")
    VideoInfoPost getVideoInfoPostByVideoId(@RequestParam String videoId);


    @RequestMapping(Constants.INNER_API_PREFIX + "/video/updateDocCount")
    void updateDocCount(@RequestParam String videoId,
                        @RequestParam SearchOrderTypeEnum searchOrderTypeEnum, @RequestParam Integer changeCount);

    @PostMapping(Constants.INNER_API_PREFIX + "/video/getVideoInfoBatch")
    Map<String, VideoInfo> getVideoInfoBatch(@RequestBody Set<String> videoIds);

    @GetMapping(Constants.INNER_API_PREFIX+ "/video/getVideoIdsByUserId")
    List<String> getVideoIdsByUserId(@RequestParam("userId") String userId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/getVideoInfoByName")
    List<VideoInfo> getVideoInfoByName(@RequestParam String videoNameFuzzy);

}