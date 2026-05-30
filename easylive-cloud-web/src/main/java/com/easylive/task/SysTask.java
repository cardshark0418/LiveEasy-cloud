package com.easylive.task;

import cn.hutool.core.date.DateUtil;
import com.easylive.config.AppConfig;
import com.easylive.enums.DateTimePatternEnum;
import com.easylive.service.StatisticsInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;

@Component
@Slf4j
public class SysTask {

    @Resource
    private StatisticsInfoService statisticsInfoService;

    @Resource
    private AppConfig appConfig;

    @Scheduled(cron = "0 0 0 * * ?")
    public void statisticsData() {
        statisticsInfoService.statisticsData();
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void delTempFile() {
        String tempFolderName = appConfig.getProjectFolder() + "file/temp/";
        File folder = new File(tempFolderName);
        File[] listFile = folder.listFiles();
        if (listFile == null) {
            return;
        }
        // DateUtil.offsetDay 是 Hutool 提供的偏移方法，-2 代表前天
        String twodaysAgo = DateUtil.format(DateUtil.offsetDay(new Date(), -2),
                DateTimePatternEnum.YYYYMMDD.getPattern()).toLowerCase();
        Integer dayInt = Integer.parseInt(twodaysAgo);
        for (File file : listFile) {
            Integer fileDate = Integer.parseInt(file.getName());
            if (fileDate <= dayInt) {
                try {
                    FileUtils.deleteDirectory(file);
                } catch (IOException e) {
                    log.info("删除临时文件失败", e);
                }
            }
        }
    }
}
