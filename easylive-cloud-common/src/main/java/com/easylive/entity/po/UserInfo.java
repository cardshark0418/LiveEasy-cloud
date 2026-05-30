package com.easylive.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息实体类
 * 对应表：user_info
 */
@TableName("user_info")
@Data
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;
//  这干啥的？
    /**
     * 用户id
     */
    @TableId(type = IdType.AUTO)
    private String userId;

    /**
     * 昵称
     */
    private String nickName;

    //头像
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 密码
     */
    private String password;

    /**
     * 性别 (0:女 1:男 2:未知)
     */
    private Integer sex;

    /**
     * 出生日期
     */
    private String birthday;

    /**
     * 学校
     */
    private String school;

    /**
     * 个人简介
     */
    private String personIntroduction;

    /**
     * 加入时间
     */
    private LocalDateTime joinTime;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 状态 (0:禁用 1:正常)
     */
    private Integer status;

    /**
     * 空间公告
     */
    private String noticeInfo;

    /**
     * 硬币总数量
     */
    private Integer totalCoinCount;

    /**
     * 当前硬币数
     */
    private Integer currentCoinCount;

    /**
     * 主题
     */
    private Integer theme;

    @TableField(exist = false)
    private Integer fansCount;
    @TableField(exist = false)
    private Integer focusCount;
    @TableField(exist = false)
    private Integer likeCount;
    @TableField(exist = false)
    private Integer playCount;
    @TableField(exist = false)
    private Boolean haveFocus;

}