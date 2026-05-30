package com.easylive.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.easylive.component.EsSearchComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.vo.VideoPlayInfoDto;
import com.easylive.enums.SearchOrderTypeEnum;
import com.easylive.redis.RedisComponent;
import com.easylive.redis.RedisUtils;
import com.easylive.service.VideoInfoPostService;
import com.easylive.service.VideoInfoService;
import com.easylive.service.VideoPlayHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class ExecuteQueueTask {

    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private VideoInfoPostService videoInfoPostService;
    @Autowired
    private VideoInfoService videoInfoService;
    @Autowired
    private RedisComponent redisComponent;
    @Resource
    private EsSearchComponent esSearchComponent;
    @Autowired
    private VideoPlayHistoryService videoPlayHistoryService;

    private ExecutorService executorService = Executors.newFixedThreadPool(2);

    @PostConstruct
    public void consumeTransferFileQueue() {
        executorService.execute(() -> {
            log.info("转码队列监听启动...");
            // 关键：检查线程中断状态
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    VideoInfoFilePost videoInfoFile = (VideoInfoFilePost) redisUtils.rpop(Constants.REDIS_KEY_QUEUE_TRANSFER);
                    if (videoInfoFile == null) {
                        Thread.sleep(1500);
                        continue;
                    }
                    videoInfoPostService.transferVideoFile(videoInfoFile);
                } catch (InterruptedException e) {
                    log.info("收到中断信号，正在退出转码任务...");
                    break; // 线程 sleep 被中断，直接退出循环
                } catch (Exception e) {
                    if (e instanceof IllegalStateException && e.getMessage().contains("destroyed")) {
                        log.warn("检测到 Redis 连接池已关闭，停止消费任务。");
                        break;
                    }
                    log.error("获取转码文件队列信息失败", e);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
            log.info("转码任务线程已安全停止。");
        });
    }

    @PostConstruct
    public void consumeVideoPlayQueue() {
        executorService.execute(() -> {
            while (true) {
                try {
                    VideoPlayInfoDto videoPlayInfoDto = (VideoPlayInfoDto) redisUtils.rpop(Constants.REDIS_KEY_QUEUE_VIDEO_PLAY);
                    if (videoPlayInfoDto == null) {
                        Thread.sleep(1500);
                        continue;
                    }
                    //更新播放数
                    videoInfoService.update(new LambdaUpdateWrapper<VideoInfo>()
                            .setSql("play_count = play_count + 1")
                            .eq(VideoInfo::getVideoId,videoPlayInfoDto.getVideoId()));
                    if (!StringUtils.isEmpty(videoPlayInfoDto.getUserId())) {
                        //记录历史
                        videoPlayHistoryService.saveHistory(videoPlayInfoDto.getUserId(), videoPlayInfoDto.getVideoId(), videoPlayInfoDto.getFileIndex());
                    }
                    //按天记录播放数
                    redisComponent.recordVideoPlayCount(videoPlayInfoDto.getVideoId());


                   //更新es播放数量
                    esSearchComponent.updateDocCount(videoPlayInfoDto.getVideoId(), SearchOrderTypeEnum.VIDEO_PLAY.getField(), 1);

                } catch (Exception e) {
                    log.error("获取视频播放文件队列信息失败", e);
                }
            }
        });
    }

    /**
     * 容器关闭前，必须杀掉线程池
     */
    @PreDestroy
    public void stop() {
        log.info("Spring 容器关闭，正在关闭线程池...");
        executorService.shutdownNow(); // 这会触发 InterruptedException 并停止 while 循环
    }
}