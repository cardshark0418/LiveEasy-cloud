package com.easylive.controller;

import com.easylive.api.consumer.WebClient;
import com.easylive.entity.po.StatisticsInfo;
import com.easylive.entity.vo.ResponseVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.*;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@RequestMapping("/index")
@Validated
public class IndexController  {

    @Resource
    private WebClient webClient;

    @RequestMapping("/getActualTimeStatisticsInfo")
    public ResponseVO getActualTimeStatisticsInfo() {
        Map result = webClient.getActualTimeStatisticsInfo();
        return getSuccessResponseVO(result);
    }

    @RequestMapping("/getWeekStatisticsInfo")
    public ResponseVO getWeekStatisticsInfo(@RequestParam Integer dataType) {
        List<StatisticsInfo> resultDataList = webClient.getWeekStatisticsInfo(dataType);
        return getSuccessResponseVO(resultDataList);
    }
}