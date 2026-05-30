package com.easylive.entity.po;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.easylive.entity.vo.UserMessageExtendDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;


/**
 * 用户消息表
 */
@Data
public class UserMessage implements Serializable {


    /**
     * 消息ID自增
     */
    private Integer messageId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 主体ID
     */
    private String videoId;

    /**
     * 消息类型
     */
    private Integer messageType;

    /**
     * 发送人ID
     */
    private String sendUserId;

    /**
     * 0:未读 1:已读
     */
    private Integer readType;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 扩展信息
     */
    @TableField(exist = false)
    private String extendJson;


    @TableField(typeHandler = JacksonTypeHandler.class,exist = false)
    private UserMessageExtendDto extendDto;

    public UserMessageExtendDto getExtendDto() {
        return StrUtil.isEmpty(extendJson) ? new UserMessageExtendDto() : JSONUtil.toBean(extendJson, UserMessageExtendDto.class);
    }

    @TableField(exist = false)
    private String videoName;

    @TableField(exist = false)
    private String videoCover;

    @TableField(exist = false)
    private String sendUserName;

    @TableField(exist = false)
    private String sendUserAvatar;





}
