package com.easylive.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;


/**
 * 视频弹幕
 */
@Data
public class VideoDanmu implements Serializable {


    /**
     * 自增ID
     */
    @TableId(type = IdType.AUTO)
    private Integer danmuId;

    /**
     * 视频ID
     */
    private String videoId;

    /**
     * 唯一ID
     */
    private String fileId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date postTime;

    /**
     * 内容
     */
    private String text;

    /**
     * 展示位置
     */
    private Integer mode;

    /**
     * 颜色
     */
    private String color;

    /**
     * 展示时间
     */
    private Integer time;

    @TableField(exist = false)
    private String videoName;
    @TableField(exist = false)
    private String videoCover;
    @TableField(exist = false)
    private String nickName;


}
