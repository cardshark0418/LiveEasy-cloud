package com.easylive.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easylive.component.EsSearchComponent;
import com.easylive.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.*;
import com.easylive.entity.query.SimplePage;
import com.easylive.entity.query.VideoInfoPostQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.SysSettingDto;
import com.easylive.entity.vo.UploadingFileDto;
import com.easylive.enums.*;
import com.easylive.exception.BusinessException;
import com.easylive.mapper.*;
import com.easylive.redis.RedisComponent;
import com.easylive.service.*;
import com.easylive.utils.FFmpegUtils;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VideoInfoPostServiceImpl extends ServiceImpl<VideoInfoPostMapper,VideoInfoPost> implements VideoInfoPostService {
    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private VideoInfoPostMapper videoInfoPostMapper;
    @Autowired
    private VideoInfoFilePostMapper videoInfoFilePostMapper;
    @Autowired
    private VideoInfoFilePostService videoInfoFilePostService;
    @Autowired
    private AppConfig appConfig;
    @Autowired
    private FFmpegUtils fFmpegUtils;
    @Autowired
    private VideoInfoMapper videoInfoMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private VideoInfoService videoInfoService;
    @Autowired
    private VideoInfoFileMapper videoInfoFileMapper;
    @Autowired
    private VideoInfoFileService videoInfoFileService;
    @Autowired
    private EsSearchComponent esSearchComponent;
    @Override
    public void saveVideoInfo(VideoInfoPost videoInfoPost, List<VideoInfoFilePost> uploadFileList) {
        //文件数量是否超出上限
        if (uploadFileList.size() > redisComponent.getSysSettingDto().getVideoPCount()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // videoId不为空 说明是修改视频
        if (!StringUtils.isEmpty(videoInfoPost.getVideoId())) {
            VideoInfoPost videoInfoPostDb = videoInfoPostMapper.selectById(videoInfoPost.getVideoId());
            if (videoInfoPostDb == null) {//修改视频但是查不到对应ID 抛异常
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            //转码中或者待审核 就不给改
            if (ArrayUtils.contains(new Integer[]{VideoStatusEnum.STATUS0.getStatus(), VideoStatusEnum.STATUS2.getStatus()}, videoInfoPostDb.getStatus())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }
        Date curDate = new Date();
        String videoId = videoInfoPost.getVideoId();
        List<VideoInfoFilePost> deleteFileList = new ArrayList();
        List<VideoInfoFilePost> addFileList = uploadFileList;
        if (StringUtils.isEmpty(videoId)) {//新增
            videoId = RandomUtil.randomString(10);
            videoInfoPost.setVideoId(videoId);
            //??
            videoInfoPost.setCreateTime(curDate);
            videoInfoPost.setLastUpdateTime(curDate);
            videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus());
            videoInfoPostMapper.insert(videoInfoPost);//第68行
        } else{  //修改
            List<VideoInfoFilePost> dbInfoFileList = videoInfoFilePostMapper.selectList(new LambdaQueryWrapper<VideoInfoFilePost>()
                    .eq(VideoInfoFilePost::getVideoId, videoId)
                    .eq(VideoInfoFilePost::getUserId, videoInfoPost.getUserId()));
            Map<String, VideoInfoFilePost> uploadFileMap =
                    uploadFileList.stream()
                            .collect(Collectors.toMap(item -> item.getUploadId(),
                                    Function.identity(),
                                    (data1, data2) -> data2));
            //删除的文件 -> 数据库中有，uploadFileList没有
            Boolean updateFileName = false;
            for (VideoInfoFilePost fileInfo : dbInfoFileList) {
                VideoInfoFilePost updateFile = uploadFileMap.get(fileInfo.getUploadId());
                if (updateFile == null) {
                    deleteFileList.add(fileInfo);
                } else if (!updateFile.getFileName().equals(fileInfo.getFileName())) {
                    updateFileName = true;
                }
            }
            //新增的文件  没有fileId就是新增的文件
            addFileList = uploadFileList.stream().filter(item -> item.getFileId() == null).collect(Collectors.toList());
            videoInfoPost.setLastUpdateTime(curDate);
            //判断视频信息是否有更改
            Boolean changeVideoInfo = this.changeVideoInfo(videoInfoPost);
            if (!addFileList.isEmpty()) {
                videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus());
            } else if (changeVideoInfo || updateFileName) {
                videoInfoPost.setStatus(VideoStatusEnum.STATUS2.getStatus());
            }
            videoInfoPostMapper.updateById(videoInfoPost);
        }

        //清除已经删除的数据
        if (!deleteFileList.isEmpty()) {
            List<String> delFileIdList = deleteFileList.stream().map(item -> item.getFileId()).collect(Collectors.toList());
            // 删表
            videoInfoFilePostMapper.delete(new LambdaQueryWrapper<VideoInfoFilePost>()
                    .eq(VideoInfoFilePost::getUserId,videoInfoPost.getUserId())
                    .in(VideoInfoFilePost::getFileId,delFileIdList));
            //将要删除的视频加入消息队列 (删磁盘)
            List<String> delFilePathList = deleteFileList.stream().map(item -> item.getFilePath()).collect(Collectors.toList());
            redisComponent.addFile2DelQueue(videoId, delFilePathList);
        }

        //更新视频信息
        Integer index = 1;
        for (VideoInfoFilePost videoInfoFile : uploadFileList) {
            videoInfoFile.setFileIndex(index++);
            videoInfoFile.setVideoId(videoId);
            videoInfoFile.setUserId(videoInfoPost.getUserId());
            if (videoInfoFile.getFileId() == null) {
                videoInfoFile.setFileId(RandomUtil.randomString(20));
                videoInfoFile.setUpdateType(VideoFileUpdateTypeEnum.UPDATE.getStatus());
                videoInfoFile.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
            }
        }
         videoInfoFilePostService.saveOrUpdateBatch(uploadFileList);
        //将需要转码的视频加入队列
        if (!addFileList.isEmpty()) {
            for (VideoInfoFilePost file : addFileList) {
                file.setUserId(videoInfoPost.getUserId());
                file.setVideoId(videoId);
            }
            redisComponent.addFile2TransferQueue(addFileList);
        }
    }

    private boolean changeVideoInfo(VideoInfoPost videoInfoPost) {
        VideoInfoPost dbInfo = videoInfoPostMapper.selectById(videoInfoPost.getVideoId());
        //标题，封面，标签，简介
        if (!videoInfoPost.getVideoCover().equals(dbInfo.getVideoCover()) || !videoInfoPost.getVideoName().equals(dbInfo.getVideoName()) || !videoInfoPost.getTags().equals(dbInfo.getTags()) || !videoInfoPost.getIntroduction().equals(
                dbInfo.getIntroduction())) {
            return true;
        }
        return false;
    }

    //从“分片文件”到“标准流媒体播放格式（HLS/m3u8）”
    @GlobalTransactional(rollbackFor = Exception.class)

    @Override
    public void transferVideoFile(VideoInfoFilePost videoInfoFile) {
        VideoInfoFilePost updateFilePost = new VideoInfoFilePost();
        try {
            UploadingFileDto fileDto = redisComponent.getUploadingVideoFile(videoInfoFile.getUserId(), videoInfoFile.getUploadId());
            /*
              拷贝文件到正式目录
             */
            String tempFilePath = appConfig.getProjectFolder() + "file/temp/" + fileDto.getFilePath();

            File tempFile = new File(tempFilePath);

            String targetFilePath = appConfig.getProjectFolder() + "file/video/" + fileDto.getFilePath();
            File taregetFile = new File(targetFilePath);
            if (!taregetFile.exists()) {
                taregetFile.mkdirs();
            }
            FileUtils.copyDirectory(tempFile, taregetFile);

            /*
             * 删除临时目录
             */
//            FileUtils.forceDelete(tempFile);
            redisComponent.delVideoFileInfo(videoInfoFile.getUserId(), videoInfoFile.getUploadId());

            /*
             * 合并文件
             */
            String completeVideo = targetFilePath + Constants.TEMP_VIDEO_NAME;
            union(targetFilePath, completeVideo, true);

            /*
             * 获取播放时长
             */
            Integer duration = fFmpegUtils.getVideoInfoDuration(completeVideo);
            updateFilePost.setDuration(duration);
            updateFilePost.setFileSize(new File(completeVideo).length());
            updateFilePost.setFilePath("video/" + fileDto.getFilePath());
            updateFilePost.setTransferResult(VideoFileTransferResultEnum.SUCCESS.getStatus());
            FileUtils.forceDelete(tempFile);
            /*
             * ffmpeg切割文件
             */
            convertVideo2Ts(completeVideo);

        } catch (Exception e) {
            log.error("文件转码失败", e);
            updateFilePost.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());
        } finally {

            //更新文件状
            videoInfoFilePostMapper.update(updateFilePost, new LambdaQueryWrapper<VideoInfoFilePost>()
                    .eq(VideoInfoFilePost::getUserId,videoInfoFile.getUserId())
                    .eq(VideoInfoFilePost::getUploadId,videoInfoFile.getUploadId()));
            //更新视频信息
            Long failCount = videoInfoFilePostMapper.selectCount(new LambdaQueryWrapper<VideoInfoFilePost>()
                    .eq(VideoInfoFilePost::getVideoId, videoInfoFile.getVideoId())
                    .eq(VideoInfoFilePost::getTransferResult,VideoFileTransferResultEnum.FAIL.getStatus()));
            if (failCount > 0) {
                VideoInfoPost videoUpdate = new VideoInfoPost();
                videoUpdate.setStatus(VideoStatusEnum.STATUS1.getStatus());
                videoInfoPostMapper.update(videoUpdate, new LambdaQueryWrapper<VideoInfoPost>().eq(VideoInfoPost::getVideoId,videoInfoFile.getVideoId()));
                return;
            }
            Long transferCount = videoInfoFilePostMapper.selectCount(new LambdaQueryWrapper<VideoInfoFilePost>()
                    .eq(VideoInfoFilePost::getVideoId, videoInfoFile.getVideoId())
                    .eq(VideoInfoFilePost::getTransferResult,VideoFileTransferResultEnum.TRANSFER.getStatus()));
            if (transferCount == 0) {
                Integer duration = videoInfoFilePostMapper.sumDuration(videoInfoFile.getVideoId());
                VideoInfoPost videoUpdate = new VideoInfoPost();
                videoUpdate.setStatus(VideoStatusEnum.STATUS2.getStatus());
                videoUpdate.setDuration(duration);
                videoInfoPostMapper.update(videoUpdate, new LambdaQueryWrapper<VideoInfoPost>().eq(VideoInfoPost::getVideoId,videoInfoFile.getVideoId()));

            }
        }

    }

    @Override
    public PaginationResultVO<VideoInfoPost> findListByPage(VideoInfoPostQuery param) {
        Integer total = videoInfoPostMapper.selectCountByQuery(param);
        int count = total == null ? 0 : total;

        int pageSize = param.getPageSize() == null ? Constants.PAGE_SIZE_15 : param.getPageSize();
        int pageNo = param.getPageNo() == null ? 1 : param.getPageNo();
        SimplePage page = new SimplePage(pageNo, count, pageSize);
        param.setSimplePage(page);

        List<VideoInfoPost> list = videoInfoPostMapper.selectListByQuery(param);

        return new PaginationResultVO<>(count, pageSize, pageNo, page.getPageTotal(), list);
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void auditVideo(String videoId, Integer status, String reason) {
        VideoStatusEnum videoStatusEnum = VideoStatusEnum.getByStatus(status);
        if (videoStatusEnum == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        VideoInfoPost videoInfoPost = new VideoInfoPost();
        videoInfoPost.setStatus(status);

        Integer audioCount = videoInfoPostMapper.update(videoInfoPost,new LambdaQueryWrapper<VideoInfoPost>()
                                                                        .eq(VideoInfoPost::getStatus,VideoStatusEnum.STATUS2.getStatus())
                                                                        .eq(VideoInfoPost::getVideoId,videoId));
        if (audioCount == 0) { //防止多人审核冲突
            throw new BusinessException("审核失败，请稍后重试");
        }

        //更新视频文件未未更新状态
        VideoInfoFilePost videoInfoFilePost = new VideoInfoFilePost();
        videoInfoFilePost.setUpdateType(VideoFileUpdateTypeEnum.NO_UPDATE.getStatus());

        videoInfoFilePostMapper.update(videoInfoFilePost,new LambdaQueryWrapper<VideoInfoFilePost>()
                .eq(VideoInfoFilePost::getVideoId,videoId));

        //审核不通过的话 直接return
        if (VideoStatusEnum.STATUS4 == videoStatusEnum) {
            return;
        }
        VideoInfoPost infoPost = this.videoInfoPostMapper.selectById(videoId);
        /**
         * 第一次发布增加用户积分
         */
        VideoInfo dbVideoInfo = this.videoInfoMapper.selectById(videoId);
        if (dbVideoInfo == null) {
            SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();

            userInfoService.update(new LambdaUpdateWrapper<UserInfo>()
                    .eq(UserInfo::getUserId,infoPost.getUserId())
                    .setSql("total_coin_count = total_coin_count + " + sysSettingDto.getPostVideoCoinCount()
                            + ", current_coin_count = current_coin_count + " + sysSettingDto.getPostVideoCoinCount()));
        }
        /**
         * 将发布信息复制到正式表信息
         */
        VideoInfo videoInfo = BeanUtil.copyProperties(infoPost,VideoInfo.class);
        videoInfoService.saveOrUpdate(videoInfo);

        //删除可能存在正式表信息 再加
        videoInfoFileMapper.delete(new LambdaQueryWrapper<VideoInfoFile>()
                .eq(VideoInfoFile::getVideoId, videoId));

        List<VideoInfoFilePost> videoInfoFilePostList = videoInfoFilePostMapper.selectList(new LambdaQueryWrapper<VideoInfoFilePost>()
                .eq(VideoInfoFilePost::getVideoId, videoId));
        List<VideoInfoFile> videoInfoFileList = BeanUtil.copyToList(videoInfoFilePostList, VideoInfoFile.class);
        videoInfoFileService.saveBatch(videoInfoFileList);
        /**
         * 审核成功 之前要删的文件可以删了
         */
        List<String> filePathList = redisComponent.getDelFileList(videoId);
        List<String> actualList = Convert.toList(String.class, filePathList);
        if (actualList != null) {
            for (String path : actualList) {
                File file = new File(appConfig.getProjectFolder() + "file/" + path);
                if (file.exists()) {
                    try {
                        FileUtils.deleteDirectory(file);
                    } catch (IOException e) {
                        log.error("删除文件失败", e);
                    }
                }
            }
        }
        redisComponent.cleanDelFileList(videoId);

        esSearchComponent.saveDoc(videoInfo);
    }

    public static void union(String dirPath, String toFilePath, boolean delSource) throws BusinessException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        File fileList[] = dir.listFiles();
        File targetFile = new File(toFilePath);
        try (RandomAccessFile writeFile = new RandomAccessFile(targetFile, "rw")) {
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                //创建读块文件的对象
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    log.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            throw new BusinessException("合并文件" + dirPath + "出错了");
        } finally {
            if (delSource) {
                for (int i = 0; i < fileList.length; i++) {
                    fileList[i].delete();
                }
            }
        }
    }


    private void convertVideo2Ts(String videoFilePath) {
        File videoFile = new File(videoFilePath);
        //创建同名切片目录
        File tsFolder = videoFile.getParentFile();
        String codec = fFmpegUtils.getVideoCodec(videoFilePath);
        //转码
        if (Constants.VIDEO_CODE_HEVC.equals(codec)) {
            String tempFileName = videoFilePath + Constants.VIDEO_CODE_TEMP_FILE_SUFFIX;
            new File(videoFilePath).renameTo(new File(tempFileName));
            fFmpegUtils.convertHevc2Mp4(tempFileName, videoFilePath);
            new File(tempFileName).delete();
        }

        //视频转为ts
        fFmpegUtils.convertVideo2Ts(tsFolder, videoFilePath);

        //删除视频文件
        videoFile.delete();
    }

    @Override
    public void recommendVideo(String videoId) {
        VideoInfo videoInfo = videoInfoMapper.selectById(videoId);
        if (videoInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        Integer recommendType = null;
        if (VideoRecommendTypeEnum.RECOMMEND.getType().equals(videoInfo.getRecommendType())) {
            recommendType = VideoRecommendTypeEnum.NO_RECOMMEND.getType();
        } else {
            recommendType = VideoRecommendTypeEnum.RECOMMEND.getType();
        }
        VideoInfo updateInfo = new VideoInfo();
        updateInfo.setRecommendType(recommendType);
        updateInfo.setVideoId(videoId);
        videoInfoMapper.updateById(updateInfo);
    }
}
