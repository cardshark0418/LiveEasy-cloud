package com.easylive.entity.vo;

import lombok.Data;

@Data
public class UserMessageCountDto {
    public Integer messageType;
    private Integer messageCount;
}