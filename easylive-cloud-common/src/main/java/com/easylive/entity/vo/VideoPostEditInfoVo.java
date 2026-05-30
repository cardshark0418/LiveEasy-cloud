package com.easylive.entity.vo;

import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.po.VideoInfoPost;
import lombok.Data;

import java.util.List;

@Data
public class VideoPostEditInfoVo {
    private VideoInfoPost videoInfo;
    private List<VideoInfoFilePost> videoInfoFileList;
}