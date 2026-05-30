package com.easylive.entity.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysSettingDto implements Serializable {
    private Integer registerCoinCount = 10;
    private Integer postVideoCoinCount = 5;
    private Integer videoSize = 100;
    private Integer videoPCount = 10;
    private Integer videoCount = 10;
    private Integer commentCount = 20;
    private Integer danmuCount = 20;
}
