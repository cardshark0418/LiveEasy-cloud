package com.easylive.api.comsumer;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.VideoInfoFile;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name =Constants.SERVER_NAME_WEB, contextId = "resourceVideoClient")
public interface VideoClient {
    @GetMapping(Constants.INNER_API_PREFIX + "/video/getVideoInfoFileByVideoId")
    VideoInfoFile getVideoInfoFileByFileId(@RequestParam("fileId") String fileId);
}