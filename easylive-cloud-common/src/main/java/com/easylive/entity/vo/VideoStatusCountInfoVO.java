package com.easylive.entity.vo;

import lombok.Data;

@Data
public class VideoStatusCountInfoVO {
    /** 前端字段：已通过 */
    private Integer auditSuccessCount;
    private Integer auditFailCount;
    /** 前端字段：进行中 */
    private Integer inProcessCount;
}
