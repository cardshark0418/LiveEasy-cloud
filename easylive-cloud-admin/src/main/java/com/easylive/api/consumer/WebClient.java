package com.easylive.api.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.StatisticsInfo;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.query.VideoInfoPostQuery;
import com.easylive.entity.vo.PaginationResultVO;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = Constants.SERVER_NAME_WEB,contextId = "adminWebClient")//调用web服务获取数据
public interface WebClient {

    @RequestMapping(Constants.INNER_API_PREFIX+"/statistics/admin/getActualTimeStatisticsInfo")
    Map getActualTimeStatisticsInfo();

    @RequestMapping(Constants.INNER_API_PREFIX+"/statistics/admin/getWeekStatisticsInfo")
    List<StatisticsInfo> getWeekStatisticsInfo(@RequestParam Integer dataType);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/getVideoCount")
    Integer getVideoCount(@RequestBody Integer categoryId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/user/loadUser")
    PaginationResultVO loadUser(@RequestParam(required = false) Integer pageNo,@RequestParam(required = false) String nickNameFuzzy,@RequestParam(required = false) Integer status);

    @RequestMapping(Constants.INNER_API_PREFIX + "/user/changeStatus")
    void changeStatus(@RequestParam String userId, @RequestParam Integer status);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/loadVideoList")
    PaginationResultVO loadVideoList(@RequestBody VideoInfoPostQuery videoInfoPostQuery);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/recommendVideo")
    void recommendVideo(@RequestParam String videoId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/auditVideo")
    Response auditVideo(@RequestParam String videoId, @RequestParam Integer status, @RequestParam String reason);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/deleteVideo")
    void deleteVideo(@RequestParam String videoId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/loadVideoPList")
    List<VideoInfoFilePost> loadVideoPList(@RequestParam String videoId);

}