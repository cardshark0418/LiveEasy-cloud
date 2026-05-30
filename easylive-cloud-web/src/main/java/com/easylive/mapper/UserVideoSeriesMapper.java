package com.easylive.mapper;

import com.easylive.entity.po.UserVideoSeries;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserVideoSeriesMapper extends MPJBaseMapper<UserVideoSeries> {

    @Update("<script>" +
            "UPDATE user_video_series " +
            "<set>" +
            "  <trim prefix='sort = CASE' suffix='END'>" +
            "    <foreach collection='list' item='item'>" +
            "      WHEN series_id = #{item.seriesId} AND user_id = #{item.userId} " +
            "      THEN #{item.sort}" +
            "    </foreach>" +
            "  </trim>" +
            "</set>" +
            "WHERE user_id = #{list[0].userId} " +
            "AND series_id IN " +
            "<foreach collection='list' item='item' open='(' separator=',' close=')'>" +
            "  #{item.seriesId}" +
            "</foreach>" +
            "</script>")
    void changeSort(@Param("list") List<UserVideoSeries> videoSeriesList);
}