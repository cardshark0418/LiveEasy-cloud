package com.easylive.agent.tool;

import cn.hutool.json.JSONUtil;
import com.easylive.agent.client.VideoClient;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.vo.PaginationResultVO;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class VideoTools {

    private final VideoClient videoClient;

    public VideoTools(VideoClient videoClient) {
        this.videoClient = videoClient;
    }

    @Tool("根据关键词搜索公开视频，返回视频标题、简介和视频编号")
    public String searchVideo(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return "搜索关键词不能为空";
        }
        try {
            PaginationResultVO<VideoInfo> result = videoClient.search(keyword.trim(), null, 1);
            return JSONUtil.toJsonStr(result);
        } catch (Exception e) {
            return "视频搜索服务暂时不可用";
        }
    }

    @Tool("根据视频编号查询视频详情，包括标题、简介、封面、标签、分类、时长和播放量")
    public String getVideoDetail(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            return "视频编号不能为空";
        }

        try {
            VideoInfo video = videoClient.getVideoInfo(videoId.trim());
            if (video == null) {
                return "没有找到这个视频";
            }
            return JSONUtil.toJsonStr(video);
        } catch (Exception e) {
            return "视频详情服务暂时不可用";
        }
    }

    @Tool("根据当前视频推荐相似视频，需要提供当前视频编号和视频关键词")
    public String recommendVideo(String videoId, String keyword) {
        if (videoId == null || videoId.trim().isEmpty()) {
            return "视频编号不能为空";
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return "推荐关键词不能为空";
        }

        try {
            return JSONUtil.toJsonStr(videoClient.getVideoRecommend(keyword.trim(), videoId.trim()));
        } catch (Exception e) {
            return "视频推荐服务暂时不可用";
        }
    }
}
