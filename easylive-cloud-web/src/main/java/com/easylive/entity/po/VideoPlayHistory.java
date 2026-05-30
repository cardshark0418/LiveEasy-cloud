package com.easylive.entity.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;


/**
 * 视频播放历史
 */
@Data
public class VideoPlayHistory implements Serializable {


    /**
     * 用户ID
     */
    private String userId;

    /**
     * 视频ID
     */
    private String videoId;

    /**
     * 文件索引
     */
    private Integer fileIndex;

    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdateTime;


    /**
     * 视频封面
     */
    @TableField(exist = false)
    private String videoCover;

    /**
     * 视频名称
     */
    @TableField(exist = false)
    private String videoName;

}
