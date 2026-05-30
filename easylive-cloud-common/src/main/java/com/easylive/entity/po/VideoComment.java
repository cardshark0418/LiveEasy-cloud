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
 * 评论
 */
@Data
public class VideoComment implements Serializable {


    /**
     * 评论ID
     */
    @TableId(type = IdType.AUTO)
    private Integer commentId;

    /**
     * 父级评论ID
     */
    private Integer pCommentId;

    /**
     * 视频ID
     */
    private String videoId;

    /**
     * 视频用户ID
     */
    private String videoUserId;

    /**
     * 回复内容
     */
    private String content;

    /**
     * 图片
     */
    private String imgPath;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 回复人ID
     */
    private String replyUserId;

    /**
     * 0:未置顶  1:置顶
     */
    private Integer topType;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date postTime;

    /**
     * 喜欢数量
     */
    private Integer likeCount;

    /**
     * 讨厌数量
     */
    private Integer hateCount;

    @TableField(exist = false)
    private List<VideoComment> children;

    @TableField(exist = false)
    private String nickName;

    @TableField(exist = false)
    private String avatar;

    @TableField(exist = false)
    private String replyNickName;

    @TableField(exist = false)
    private String replyAvatar;

    @TableField(exist = false)
    private String videoName;

    @TableField(exist = false)
    private String videoCover;


}
