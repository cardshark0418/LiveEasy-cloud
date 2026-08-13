package com.easylive.agent.client;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.vo.PaginationResultVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Agent 调用视频服务的客户端。 */
@FeignClient(name = Constants.SERVER_NAME_WEB, contextId = "agentVideoClient")
public interface VideoClient {

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/search")
    PaginationResultVO<VideoInfo> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "orderType", required = false) Integer orderType,
            @RequestParam(value = "pageNo", required = false) Integer pageNo);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/getVideoInfoByVideoId")
    VideoInfo getVideoInfo(@RequestParam("videoId") String videoId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/getVideoRecommend")
    java.util.List<VideoInfo> getVideoRecommend(
            @RequestParam("keyword") String keyword,
            @RequestParam("videoId") String videoId);
}
