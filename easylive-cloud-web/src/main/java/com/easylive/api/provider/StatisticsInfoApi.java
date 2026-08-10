package com.easylive.api.provider;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.StatisticsInfo;
import com.easylive.entity.po.UserInfo;
import com.easylive.enums.StatisticsTypeEnum;
import com.easylive.mapper.StatisticsInfoMapper;
import com.easylive.mapper.UserInfoMapper;
import com.easylive.service.StatisticsInfoService;
import com.easylive.service.UserInfoService;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping(Constants.INNER_API_PREFIX + "/statistics/admin")
public class StatisticsInfoApi {

    @Resource
    private StatisticsInfoService statisticsInfoService;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private StatisticsInfoMapper statisticsInfoMapper;

    @Resource
    private UserInfoMapper userInfoMapper;

    @RequestMapping("/getActualTimeStatisticsInfo")
    Map getActualTimeStatisticsInfo(){
        String preDate = DateUtil.format(DateUtil.yesterday(), "yyyyMMdd");
        List<StatisticsInfo> preDayData = statisticsInfoService.getPreDayTotalInfo(preDate);
        //查询用户总数，替换类型为粉丝的数量
        Integer userCount = Math.toIntExact(userInfoService.count(new LambdaQueryWrapper<UserInfo>()));
        preDayData.forEach(item -> {
            if (StatisticsTypeEnum.FANS.getType().equals(item.getDataType())) {
                item.setStatisticsCount(userCount);
            }
        });
        Map<Integer, Integer> preDayDataMap = preDayData.stream().collect(Collectors.toMap(StatisticsInfo::getDataType, StatisticsInfo::getStatisticsCount, (item1,
                                                                                                                                                             item2) -> item2));
        Map<String, Object> totalCountInfo = statisticsInfoService.getStatisticsInfoActualTime(null);
        Map<String, Object> result = new HashMap<>();
        result.put("preDayData", preDayDataMap);
        result.put("totalCountInfo", totalCountInfo);
        return result;
    }

    @RequestMapping("/getWeekStatisticsInfo")
    List<StatisticsInfo> getWeekStatisticsInfo(@RequestParam Integer dataType){
        // 1. 生成最近 7 天日期列表 (yyyyMMdd)
        DateTime start = DateUtil.offsetDay(new Date(), -7);
        DateTime end = DateUtil.yesterday();
        List<String> dateList = DateUtil.rangeToList(start, end, DateField.DAY_OF_YEAR)
                .stream()
                .map(date -> DateUtil.format(date, "yyyyMMdd"))
                .collect(Collectors.toList());

        List<StatisticsInfo> statisticsInfoList;
        String startDate = dateList.get(0);
        String endDate = dateList.get(dateList.size() - 1);

        // 2. 根据类型选择不同的统计源
        if (!StatisticsTypeEnum.FANS.getType().equals(dataType)) {
            // 分支 A: 统计表求和 (对应原 selectListTotalInfoByParam)
            statisticsInfoList = statisticsInfoMapper.selectJoinList(StatisticsInfo.class,
                    new MPJLambdaWrapper<StatisticsInfo>()
                            .select(StatisticsInfo::getStatisticsDate, StatisticsInfo::getDataType)
                            .selectSum(StatisticsInfo::getStatisticsCount, StatisticsInfo::getStatisticsCount)
                            .between(StatisticsInfo::getStatisticsDate, startDate, endDate)
                            .eq(StatisticsInfo::getDataType, dataType)
                            .groupBy(StatisticsInfo::getStatisticsDate, StatisticsInfo::getDataType));
        } else {
            // 分支 B: 用户表实时计数 (对应原 selectUserCountTotalInfoByParam)
            statisticsInfoList = userInfoMapper.selectJoinList(StatisticsInfo.class,
                    new MPJLambdaWrapper<UserInfo>()
                            .selectCount(UserInfo::getUserId, StatisticsInfo::getStatisticsCount)
                            // 注意格式必须与 dateList 一致: yyyyMMdd
                            .selectAs("DATE_FORMAT(join_time, '%Y%m%d')", StatisticsInfo::getStatisticsDate)
                            .apply("DATE_FORMAT(join_time, '%Y%m%d') BETWEEN {0} AND {1}", startDate, endDate)
                            .groupBy("DATE_FORMAT(join_time, '%Y%m%d')"));
        }

        // 3. 将结果转为 Map，方便按日期补全 0
        Map<String, StatisticsInfo> dataMap = statisticsInfoList.stream()
                .collect(Collectors.toMap(StatisticsInfo::getStatisticsDate, Function.identity(), (d1, d2) -> d2));

        // 4. 补全逻辑：确保 7 天每天都有数据，没数据的日期补 0
        List<StatisticsInfo> resultDataList = new ArrayList<>();
        for (String date : dateList) {
            StatisticsInfo dataItem = dataMap.get(date);
            if (dataItem == null) {
                dataItem = new StatisticsInfo();
                dataItem.setStatisticsCount(0);
                dataItem.setStatisticsDate(date);
                dataItem.setDataType(dataType);
            }
            resultDataList.add(dataItem);
        }
        return resultDataList;
    }

}