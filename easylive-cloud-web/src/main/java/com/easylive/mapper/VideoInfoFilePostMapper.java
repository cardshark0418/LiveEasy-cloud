package com.easylive.mapper;

import com.easylive.entity.po.VideoInfoFilePost;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VideoInfoFilePostMapper extends MPJBaseMapper<VideoInfoFilePost> {
    @Select("select ifnull(sum(duration), 0) from video_info_file_post where video_id = #{videoId}")
    Integer sumDuration(@Param("videoId") String videoId);
}
