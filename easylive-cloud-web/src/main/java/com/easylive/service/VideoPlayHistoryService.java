package com.easylive.service;

import com.easylive.entity.po.VideoPlayHistory;
import com.github.yulichang.base.MPJBaseService;

public interface VideoPlayHistoryService extends MPJBaseService<VideoPlayHistory> {
    void saveHistory(String userId, String videoId, Integer fileIndex);
}