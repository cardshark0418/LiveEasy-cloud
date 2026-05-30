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
 * 粉丝，关注表
 */
@Data
public class UserFocus implements Serializable {


    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private String userId;

    /**
     * 关注用户ID
     */
    private String focusUserId;

    /**
     * 关注时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date focusTime;

    @TableField(exist = false)
    private String otherNickName;
    @TableField(exist = false)
    private String otherUserId;
    @TableField(exist = false)
    private String otherPersonIntroduction;
    @TableField(exist = false)
    private String otherAvatar;
    @TableField(exist = false)
    private Integer focusType;

}
