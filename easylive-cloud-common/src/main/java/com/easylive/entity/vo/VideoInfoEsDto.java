package com.easylive.entity.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class VideoInfoEsDto {


    /**
     * 视频ID
     */
    private String videoId;

    /**
     * 视频封面
     */
    private String videoCover;

    /**
     * 视频名称
     */
    private String videoName;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 创建时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 标签
     */
    private String tags;

    /**
     * 播放数量
     */
    private Integer playCount;

    /**
     * 弹幕数量
     */
    private Integer danmuCount;

    /**
     * 收藏数量
     */
    private Integer collectCount;

    /** 视频标题、标签和简介生成的语义向量。 */
    private List<Float> videoVector;
}
