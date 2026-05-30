package com.easylive.entity.vo;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserMessageExtendDto {
    private String messageContent;

    private String messageContentReply;

    //审核状态
    private Integer auditStatus;

}