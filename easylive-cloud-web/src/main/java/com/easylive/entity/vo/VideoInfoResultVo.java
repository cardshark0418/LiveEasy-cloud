package com.easylive.entity.vo;

import com.easylive.entity.po.UserAction;
import com.easylive.entity.po.VideoInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class VideoInfoResultVo {
    private VideoInfo videoInfo;
    private List<UserAction> userActionList;

    public VideoInfoResultVo(VideoInfo videoInfo) {
        this.videoInfo = videoInfo;
    }

    public VideoInfoResultVo() {
    }

}
