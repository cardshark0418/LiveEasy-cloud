package com.easylive.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.easylive.enums.VideoStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;



@Data
public class VideoInfoPost extends VideoInfo implements Serializable {


    /**
     * 视频ID
     */
    @TableField("video_id")
    @TableId(type = IdType.INPUT)
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdateTime;

    /**
     * 父级分类ID
     */
    private Integer pCategoryId;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 0:转码中 1转码失败 2:待审核 3:审核成功 4:审核失败
     */
    private Integer status;

    /**
     * 0:自制作  1:转载
     */
    private Integer postType;

    /**
     * 原资源说明
     */
    private String originInfo;

    /**
     * 标签
     */
    private String tags;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 互动设置
     */
    private String interaction;

    /**
     * 持续时间（秒）
     */
    private Integer duration;

    @TableField(exist = false)
    private Integer playCount;

    @TableField(exist = false)
    private Integer likeCount;

    @TableField(exist = false)
    private Integer danmuCount;

    @TableField(exist = false)
    private Integer commentCount;

    @TableField(exist = false)
    private Integer coinCount;

    @TableField(exist = false)
    private Integer collectCount;

    @TableField(exist = false)
    private Integer recommendType;

    @TableField(exist = false)
    private Date lastPlayTime;

    // 还有这三个额外属性（截图显示表中也没有）
    @TableField(exist = false)
    private String nickName;

    @TableField(exist = false)
    private String avatar;

    @TableField(exist = false)
    private String categoryFullName;

    @TableField(exist = false)
    private String statusName;

    public String getStatusName(){
        VideoStatusEnum status1 = VideoStatusEnum.getByStatus(status);
        return status1 == null?"":status1.getDesc();
    }
}