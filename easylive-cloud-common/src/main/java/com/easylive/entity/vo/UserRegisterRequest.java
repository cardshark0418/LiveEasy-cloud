package com.easylive.entity.vo;

import com.easylive.entity.constants.Constants;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UserRegisterRequest {

    @NotEmpty(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 150)
    private String email;

    @NotEmpty(message = "密码不能为空")
    @Size(min = 8, max = 18, message = "密码长度必须在8-18位之间")
    @Pattern(regexp = Constants.PASSWORD_REGEXP, message = "密码必须包含字母和数字")
    private String registerPassword;

    @NotEmpty(message = "昵称不能为空")
    @Size(max = 20)
    private String nickName;

    @NotEmpty(message = "验证码Key不能为空")
    private String checkCodeKey;

    @NotEmpty(message = "验证码不能为空")
    private String checkCode;
}