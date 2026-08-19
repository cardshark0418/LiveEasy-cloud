package com.easylive.agent.tool;

import cn.hutool.json.JSONUtil;
import com.easylive.agent.client.VideoClient;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.vo.PaginationResultVO;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class VideoTools {

    private final VideoClient videoClient;

    public VideoTools(VideoClient videoClient) {
        this.videoClient = videoClient;
    }

    @Tool("根据关键词搜索公开视频，返回视频标题、简介、视频编号和可直接使用的 Markdown 视频链接")
    public String searchVideo(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return "搜索关键词不能为空";
        }
        try {
            PaginationResultVO<VideoInfo> result = videoClient.search(keyword.trim(), null, 1);
            return buildVideoListJson(result == null ? null : result.getList());
        } catch (Exception e) {
            return "视频搜索服务暂时不可用";
        }
    }

    @Tool("根据视频编号查询视频详情，包括标题、简介、封面、标签、分类、时长、播放量和可直接使用的 Markdown 视频链接")
    public String getVideoDetail(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            return "视频编号不能为空";
        }

        try {
            VideoInfo video = videoClient.getVideoInfo(videoId.trim());
            if (video == null) {
                return "没有找到这个视频";
            }
            return JSONUtil.parseObj(video)
                    .set("markdownLink", buildMarkdownLink(video))
                    .toString();
        } catch (Exception e) {
            return "视频详情服务暂时不可用";
        }
    }

    @Tool("根据当前视频推荐相似视频，需要提供当前视频编号和视频关键词，返回视频信息和可直接使用的 Markdown 视频链接")
    public String recommendVideo(String videoId, String keyword) {
        if (videoId == null || videoId.trim().isEmpty()) {
            return "视频编号不能为空";
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return "推荐关键词不能为空";
        }

        try {
            return buildVideoListJson(videoClient.getVideoRecommend(keyword.trim(), videoId.trim()));
        } catch (Exception e) {
            return "视频推荐服务暂时不可用";
        }
    }

    private String buildVideoListJson(List<VideoInfo> videos) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (videos != null) {
            for (VideoInfo video : videos) {
                if (video == null || video.getVideoId() == null || video.getVideoName() == null) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", video.getVideoName());
                item.put("videoId", video.getVideoId());
                item.put("introduction", video.getIntroduction());
                item.put("nickName", video.getNickName());
                item.put("markdownLink", buildMarkdownLink(video));
                result.add(item);
            }
        }
        return JSONUtil.toJsonStr(result);
    }

    private String buildMarkdownLink(VideoInfo video) {
        return "[" + video.getVideoName() + "](/video/" + video.getVideoId() + ")";
    }
}
