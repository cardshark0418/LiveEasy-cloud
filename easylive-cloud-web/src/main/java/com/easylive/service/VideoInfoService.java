package com.easylive.service;

import com.easylive.entity.po.VideoInfo;
import com.github.yulichang.base.MPJBaseService;

import javax.validation.constraints.NotEmpty;

public interface VideoInfoService extends MPJBaseService<VideoInfo> {
    void changeInteraction(@NotEmpty String videoId, String userId, String interaction);

    void deleteVideo(@NotEmpty String videoId, String userId);
}
