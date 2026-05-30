package com.easylive.enums;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum StatisticsTypeEnum {

    PLAY(0, "播放量"),
    FANS(1, "粉丝"),
    LIKE(2, "点赞"),
    COLLECTION(3, "收藏"),
    COIN(4, "投币"),
    COMMENT(5, "评论"),
    DANMU(6, "弹幕");

    private Integer type;
    @Setter
    private String desc;

    StatisticsTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static StatisticsTypeEnum getByType(Integer type) {
        for (StatisticsTypeEnum item : StatisticsTypeEnum.values()) {
            if (item.getType().equals(type)) {
                return item;
            }
        }
        return null;
    }

}
