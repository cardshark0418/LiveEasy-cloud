package com.easylive.service.impl;

import com.easylive.entity.po.VideoPlayHistory;
import com.easylive.mapper.VideoPlayHistoryMapper;
import com.easylive.service.VideoPlayHistoryService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class VideoPlayHistoryServiceImpl extends MPJBaseServiceImpl<VideoPlayHistoryMapper, VideoPlayHistory> implements VideoPlayHistoryService {

    @Autowired
    private VideoPlayHistoryMapper videoPlayHistoryMapper;

    @Override
    public void saveHistory(String userId, String videoId, Integer fileIndex) {
        VideoPlayHistory history = new VideoPlayHistory();
        history.setVideoId(videoId);
        history.setFileIndex(fileIndex);
        history.setUserId(userId);
        history.setLastUpdateTime(new Date());
        videoPlayHistoryMapper.insertOrUpdate(history);
    }
}