package com.easylive.api.consumer;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.StatisticsInfo;
import com.easylive.entity.po.UserAction;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient(name="easylive-cloud-interact", contextId = "userActionClient")
public interface UserActionClient {


    @RequestMapping(Constants.INNER_API_PREFIX+"/userAction"+"/getUserActionListWithVideo")
    List<UserAction> getUserActionListWithVideo(@RequestParam String videoId, @RequestParam String userId);

    @RequestMapping(Constants.INNER_API_PREFIX+"/userAction"+"/deleteVideoDanmu")
    void deleteVideoDanmu(@RequestParam String videoId);

    @RequestMapping(Constants.INNER_API_PREFIX+"/userAction"+"/deleteVideoComment")
    void deleteVideoComment(@RequestParam String videoId);

    @RequestMapping(Constants.INNER_API_PREFIX+"/userAction"+"/selectStatisticsComment")
    List<StatisticsInfo> selectStatisticsComment(@RequestParam String begin,@RequestParam String end);

    @RequestMapping(Constants.INNER_API_PREFIX+"/userAction"+"/selectStatisticsInfo")
    List<StatisticsInfo> selectStatisticsInfo(@RequestParam String begin,@RequestParam String end,@RequestParam Integer[] actionTypes);
}

