package com.easylive.service;

import com.easylive.entity.po.VideoDanmu;
import com.github.yulichang.base.MPJBaseService;

import javax.validation.constraints.NotNull;

public interface VideoDanmuService extends MPJBaseService<VideoDanmu> {
    void saveDanmu(VideoDanmu videoDanmu);

    void deleteDanmu(String userId, @NotNull Integer danmuId);
}