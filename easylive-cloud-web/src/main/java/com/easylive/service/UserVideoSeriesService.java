package com.easylive.service;

import com.easylive.entity.po.UserVideoSeries;
import com.github.yulichang.base.MPJBaseService;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public interface UserVideoSeriesService extends MPJBaseService<UserVideoSeries> {

    void saveUserVideoSeries(UserVideoSeries videoSeries, String videoIds);

    void saveSeriesVideo(String userId, @NotNull Integer seriesId, @NotEmpty String videoIds);

    void delSeriesVideo(String userId, @NotNull Integer seriesId, @NotEmpty String videoId);

    void delVideoSeries(String userId, @NotNull Integer seriesId);

    void changeVideoSeriesSort(String userId, @NotEmpty String seriesIds);
}