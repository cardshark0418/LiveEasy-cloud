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
 * 用户行为 点赞、评论
 */
@Data
public class UserAction implements Serializable {


    /**
     * 自增ID
     */
    @TableId(type = IdType.AUTO)
    private Integer actionId;

    /**
     * 视频ID
     */
    private String videoId;

    /**
     * 视频用户ID
     */
    private String videoUserId;

    /**
     * 评论ID
     */
    private Integer commentId;

    /**
     * 0:评论喜欢点赞 1:讨厌评论 2:视频点赞 3:视频收藏 4:视频投币
     */
    private Integer actionType;

    /**
     * 数量
     */
    private Integer actionCount;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date actionTime;

    @TableField(exist = false)
    private String videoName;

    @TableField(exist = false)
    private String videoCover;
}
