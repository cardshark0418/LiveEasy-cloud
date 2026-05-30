package com.easylive.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

@Data
public class VideoInfoFilePost implements Serializable {


    /**
     * 自增唯一ID
     */
    @TableId(type = IdType.INPUT)
    private String fileId;

    /**
     * 上传ID
     */
    private String uploadId;

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
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 0:无更新 1:有更新
     */
    private Integer updateType;

    /**
     * 0:转码中 1:转码成功 2:转码失败
     */
    private Integer transferResult;

    /**
     * 持续时间（秒）
     */
    private Integer duration;
}