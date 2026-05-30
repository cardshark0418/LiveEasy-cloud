package com.easylive.service.impl;

import com.easylive.config.AppConfig;
import com.easylive.exception.BusinessException;
import com.easylive.service.MediaProcessService;
import com.easylive.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 音视频处理服务
 * 负责视频文件的音频提取等媒体处理任务
 */
@Service
@Slf4j
public class MediaProcessServiceImpl implements MediaProcessService {


    @Resource
    private AppConfig appConfig;

    /**
     * 从m3u8视频中异步提取音频
     * 
     * @param m3u8FilePath m3u8索引文件的绝对路径
     * @return 提取后的音频文件绝对路径
     * @throws BusinessException 当提取失败时抛出
     */
    public String extractAudioFromM3u8(String m3u8FilePath) {
        long startTime = System.currentTimeMillis();
        
        if (StringUtils.isBlank(m3u8FilePath)) {
            throw new BusinessException("视频文件路径不能为空");
        }

        File m3u8File = new File(m3u8FilePath);
        if (!m3u8File.exists()) {
            log.error("视频文件不存在: {}", m3u8FilePath);
            throw new BusinessException("视频文件不存在");
        }

        // 生成音频输出路径（与m3u8同目录，命名为audio.m4a）
        String audioPath = m3u8File.getParent() + File.separator + "audio.m4a";
        
        // FFmpeg命令：从m3u8提取音频，使用AAC编码，比特率128k
        String cmd = String.format(
            "ffmpeg -i \"%s\" -vn -acodec aac -ab 128k -y \"%s\"",
            m3u8FilePath,
            audioPath
        );

        log.info("开始提取音频 - 源文件: {}, 目标文件: {}", m3u8FilePath, audioPath);
        log.info("执行FFmpeg命令: {}", cmd);

        try {
            // 执行FFmpeg命令，带超时控制
            String result = executeWithTimeout(cmd, 300); // 5分钟超时
            
            // 验证音频文件是否生成成功
            File audioFile = new File(audioPath);
            if (!audioFile.exists() || audioFile.length() == 0) {
                log.error("音频提取失败，文件未生成或为空 - 源文件: {}", m3u8FilePath);
                throw new BusinessException("音频提取失败");
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("音频提取成功 - 耗时: {}ms, 音频文件: {}, 大小: {}KB",
                duration, audioPath, audioFile.length() / 1024);
            
            return audioPath;
            
        } catch (BusinessException e) {
            log.error("音频提取失败 - 源文件: {}, 错误: {}", m3u8FilePath, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("音频提取异常 - 源文件: {}, 异常信息: {}", m3u8FilePath, e.getMessage(), e);
            throw new BusinessException("音频提取过程发生异常: " + e.getMessage());
        }
    }

    /**
     * 带超时控制的命令执行
     * 
     * @param cmd 要执行的命令
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行结果
     */
    private String executeWithTimeout(String cmd, int timeoutSeconds) {
        Thread executionThread = new Thread(() -> {
            try {
                ProcessUtils.executeCommand(cmd, appConfig.getShowFFmpegLog());
            } catch (Exception e) {
                log.error("命令执行线程异常: {}", e.getMessage());
            }
        });

        executionThread.start();
        
        try {
            // 等待执行完成或超时
            executionThread.join(TimeUnit.SECONDS.toMillis(timeoutSeconds));
            
            if (executionThread.isAlive()) {
                executionThread.interrupt();
                log.error("FFmpeg执行超时 - 超时时间: {}秒", timeoutSeconds);
                throw new BusinessException("音频提取超时");
            }
            
            return "success";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待FFmpeg执行被中断: {}", e.getMessage());
            throw new BusinessException("音频提取被中断");
        }
    }
}