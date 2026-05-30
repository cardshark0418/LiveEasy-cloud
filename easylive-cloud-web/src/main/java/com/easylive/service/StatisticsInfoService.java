package com.easylive.service;

import com.easylive.entity.po.StatisticsInfo;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;
import java.util.Map;

public interface StatisticsInfoService extends MPJBaseService<StatisticsInfo> {
    void statisticsData();

    List<StatisticsInfo> selectStatisticsFans(String statisticsDate);

    Map<String, Object> getStatisticsInfoActualTime(String userId);

    List<StatisticsInfo> getPreDayTotalInfo(String preDate);
}