package com.easylive.controller;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.annotation.GlobalInterceptor;
import com.easylive.entity.po.StatisticsInfo;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.UserLoginDto;
import com.easylive.redis.RedisComponent;
import com.easylive.service.StatisticsInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@Validated
@RequestMapping("/ucenter")
public class UCenterStatisticsController {

    @Resource
    private StatisticsInfoService statisticsInfoService;
    @Resource
    private RedisComponent redisComponent;

    @RequestMapping("/getActualTimeStatisticsInfo")
    @GlobalInterceptor
    public ResponseVO getActualTimeStatisticsInfo(HttpServletRequest request) {

        String preDate = DateUtil.formatDate(DateUtil.yesterday());
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        List<StatisticsInfo> preDayData = statisticsInfoService.list(new LambdaQueryWrapper<StatisticsInfo>()
                .eq(StatisticsInfo::getUserId,tokenUserInfoDto.getUserId())
                .eq(StatisticsInfo::getStatisticsDate,preDate));
        Map<Integer, Integer> preDayDataMap = preDayData.stream().collect(Collectors.toMap(StatisticsInfo::getDataType, StatisticsInfo::getStatisticsCount, (item1, item2) -> item2));
        Map<String, Object> totalCountInfo = statisticsInfoService.getStatisticsInfoActualTime(tokenUserInfoDto.getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("preDayData", preDayDataMap);
        result.put("totalCountInfo", totalCountInfo);
        return getSuccessResponseVO(result);
    }

    @RequestMapping("/getWeekStatisticsInfo")
    @GlobalInterceptor
    public ResponseVO getWeekStatisticsInfo(Integer dataType,HttpServletRequest request) {
        UserLoginDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(request);
        DateTime start = DateUtil.offsetDay(DateUtil.date(), -7);
        DateTime end = DateUtil.yesterday();

        List<String> dateList = DateUtil.rangeToList(start, end, DateField.DAY_OF_YEAR)
                .stream()
                .map(date -> DateUtil.format(date, "yyyyMMdd"))
                .collect(Collectors.toList());

        List<StatisticsInfo> statisticsInfoList = statisticsInfoService.list(new LambdaQueryWrapper<StatisticsInfo>()
                .eq(StatisticsInfo::getDataType,dataType)
                .eq(StatisticsInfo::getUserId,tokenUserInfoDto.getUserId())
                .between(StatisticsInfo::getStatisticsDate,dateList.get(0),dateList.get(dateList.size() - 1))
                .orderByAsc(StatisticsInfo::getStatisticsDate));

        Map<String, StatisticsInfo> dataMap = statisticsInfoList.stream().collect(Collectors.toMap(item -> item.getStatisticsDate(), Function.identity(), (data1, data2) -> data2));
        List<StatisticsInfo> resultDataList = new ArrayList<>();
        for (String date : dateList) {
            StatisticsInfo dataItem = dataMap.get(date);
            if (dataItem == null) {
                dataItem = new StatisticsInfo();
                dataItem.setStatisticsCount(0);
                dataItem.setStatisticsDate(date);
            }
            resultDataList.add(dataItem);
        }
        return getSuccessResponseVO(resultDataList);
    }

}