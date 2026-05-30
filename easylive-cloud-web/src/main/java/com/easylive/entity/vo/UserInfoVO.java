package com.easylive.entity.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户信息
 */
@Data
public class UserInfoVO implements Serializable {


    /**
     * 用户ID
     */
    private String userId;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 0:女 1:男
     */
    private Integer sex;

    /**
     * 个人简介
     */
    private String personIntroduction;

    /**
     * 空间公告
     */
    private String noticeInfo;

    /**
     * 等级
     */
    private Integer grade;

    /**
     * 出生日期
     */
    private String birthday;

    /**
     * 学校
     */
    private String school;

    private Integer fansCount;

    private Integer focusCount;

    private Integer likeCount;

    private Integer playCount;

    private Boolean haveFocus;

    private Integer theme;
}