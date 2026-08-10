package com.easylive.mapper;

import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.query.VideoInfoPostQuery;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VideoInfoPostMapper extends MPJBaseMapper<VideoInfoPost> {

    String LIST_FROM_WHERE =
            "FROM video_info_post v " +
            "<if test='query.queryCountInfo or query.recommendType != null'>" +
            "  LEFT JOIN video_info c ON c.video_id = v.video_id " +
            "</if>" +
            "<if test='query.queryUserInfo'>" +
            "  LEFT JOIN user_info u ON u.user_id = v.user_id " +
            "</if>" +
            "<where>" +
            "  <if test='query.userId != null and query.userId != \"\"'> AND v.user_id = #{query.userId} </if>" +
            "  <if test='query.videoNameFuzzy != null and query.videoNameFuzzy != \"\"'> AND v.video_name LIKE CONCAT('%', #{query.videoNameFuzzy}, '%') </if>" +
            "  <if test='query.status != null'> AND v.status = #{query.status} </if>" +
            "  <if test='query.categoryId != null'> AND v.category_id = #{query.categoryId} </if>" +
            "  <if test='query.pCategoryId != null'> AND v.p_category_id = #{query.pCategoryId} </if>" +
            "  <if test='query.recommendType != null'> AND c.recommend_type = #{query.recommendType} </if>" +
            "  <if test='query.excludeStatusArray != null'> " +
            "    AND v.status NOT IN <foreach collection='query.excludeStatusArray' item='item' open='(' separator=',' close=')'>#{item}</foreach> " +
            "  </if>" +
            "</where>";

    @Select("<script>" +
            "SELECT v.* " +
            "<if test='query.queryCountInfo'>" +
            "  ,c.play_count, c.like_count, c.danmu_count, c.comment_count, c.coin_count, c.collect_count, c.recommend_type " +
            "</if>" +
            "<if test='query.queryUserInfo'>" +
            "  ,u.nick_name, u.avatar " +
            "</if>" +
            LIST_FROM_WHERE +
            "<if test='query.orderBy != null'> ORDER BY ${query.orderBy} </if>" +
            "<if test='query.simplePage != null'> LIMIT #{query.simplePage.start}, #{query.simplePage.end} </if>" +
            "</script>")
    List<VideoInfoPost> selectListByQuery(@Param("query") VideoInfoPostQuery param);

    @Select("<script>" +
            "SELECT COUNT(1) " +
            LIST_FROM_WHERE +
            "</script>")
    Integer selectCountByQuery(@Param("query") VideoInfoPostQuery param);
}
