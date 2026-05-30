package com.easylive.entity.vo;

import com.easylive.entity.po.UserVideoSeries;
import com.easylive.entity.po.UserVideoSeriesVideo;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class UserVideoSeriesDetailVO {
    private UserVideoSeries videoSeries;
    private List<UserVideoSeriesVideo> seriesVideoList;

    public UserVideoSeriesDetailVO() {

    }

    public UserVideoSeriesDetailVO(UserVideoSeries videoSeries, List<UserVideoSeriesVideo> seriesVideoList) {
        this.videoSeries = videoSeries;
        this.seriesVideoList = seriesVideoList;
    }

}
