package com.easylive.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * 用户视频序列归档
 */
@Data
public class UserVideoSeries implements Serializable {


    /**
     * 列表ID
     */
    @TableId(type = IdType.AUTO)
    private Integer seriesId;

    /**
     * 列表名称
     */
    private String seriesName;

    /**
     * 描述
     */
    private String seriesDescription;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @TableField(exist = false)
    private String cover;

    /**
     * 专题下的视频
     */
    @TableField(exist = false)
    private List<VideoInfo> videoInfoList;

}
