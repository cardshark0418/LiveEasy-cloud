package com.easylive.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * 视频文件信息
 */
@Data
public class VideoInfoFile implements Serializable {

    /**
     * 唯一ID
     */
    @TableId
    private String fileId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 视频ID
     */
    private String videoId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件索引
     */
    private Integer fileIndex;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 持续时间（秒）
     */
    private Integer duration;
}