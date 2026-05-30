package com.easylive.entity.vo;

import lombok.Data;

@Data
public class VideoStatusCountInfoVO {
    private Integer auditPassCount;
    private Integer auditFailCount;
    private Integer inProgress;
}