package com.easylive.entity.po;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 *
 */
@Data
public class UserVideoSeriesVideo implements Serializable {


    /**
     * 列表ID
     */
    private Integer seriesId;

    /**
     * 视频ID
     */
    private String videoId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 排序
     */
    private Integer sort;

    @TableField(exist = false)
    private String videoCover;

    @TableField(exist = false)
    private String videoName;

    @TableField(exist = false)
    private Integer playCount;

    @TableField(exist = false)
    private Date createTime;

}
