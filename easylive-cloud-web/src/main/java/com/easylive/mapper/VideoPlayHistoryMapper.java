package com.easylive.mapper;

import com.easylive.entity.po.VideoPlayHistory;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VideoPlayHistoryMapper extends MPJBaseMapper<VideoPlayHistory> {
    @Insert("INSERT INTO video_play_history (user_id, video_id, file_index, last_update_time) " +
            "VALUES (#{h.userId}, #{h.videoId}, #{h.fileIndex}, #{h.lastUpdateTime}) " +
            "ON DUPLICATE KEY UPDATE " +
            "file_index = VALUES(file_index), " +
            "last_update_time = VALUES(last_update_time)")
    int insertOrUpdate(@Param("h") VideoPlayHistory history);
}