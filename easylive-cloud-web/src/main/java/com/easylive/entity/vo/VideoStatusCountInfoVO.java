package com.easylive.entity.vo;

import lombok.Data;

@Data
public class VideoStatusCountInfoVO {
    private Integer auditSuccessCount;
    private Integer auditFailCount;
    private Integer inProcessCount;
}
