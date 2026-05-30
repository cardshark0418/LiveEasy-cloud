package com.easylive.entity.query;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseParam {
    private SimplePage simplePage;
    private Integer pageNo;
    private Integer pageSize;
    private String orderBy;
}
