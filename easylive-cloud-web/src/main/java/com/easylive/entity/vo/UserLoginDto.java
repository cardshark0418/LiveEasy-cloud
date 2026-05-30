package com.easylive.entity.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录令牌信息传输对象
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLoginDto implements Serializable {

    private static final long serialVersionUID = 9170480547933408839L;

    private String userId;        // 用户唯一ID
    private String nickName;      // 用户昵称
    private String avatar;        // 用户头像路径
    private Long expireAt;        // 令牌过期时间戳
    private String token;         // 登录校验令牌

    private Integer fansCount;         // 粉丝数
    private Integer currentCoinCount;  // 当前硬币数
    private Integer focusCount;        // 关注数
    private Integer status;
}