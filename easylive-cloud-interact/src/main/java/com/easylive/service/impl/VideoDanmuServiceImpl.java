package com.easylive.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.easylive.api.consumer.VideoClient;
import com.easylive.entity.po.VideoDanmu;
import com.easylive.entity.po.VideoInfo;
import com.easylive.enums.ResponseCodeEnum;
import com.easylive.enums.SearchOrderTypeEnum;
import com.easylive.enums.UserActionTypeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.mapper.VideoDanmuMapper;
import com.easylive.service.VideoDanmuService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VideoDanmuServiceImpl extends MPJBaseServiceImpl<VideoDanmuMapper, VideoDanmu> implements VideoDanmuService {

//    @Autowired
//    private VideoInfoMapper videoInfoMapper;
    @Autowired
    private VideoDanmuMapper videoDanmuMapper;
//    @Autowired
//    private EsSearchComponent esSearchComponent;
    @Autowired
    private VideoClient videoClient;


    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void saveDanmu(VideoDanmu bean) {
//        VideoInfo videoInfo = videoInfoMapper.selectById(bean.getVideoId());
        VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(bean.getVideoId());
        if (videoInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //是否关闭弹幕
        if (videoInfo.getInteraction() != null && videoInfo.getInteraction().contains(Constants.ONE)) {
            throw new BusinessException("UP主已关闭弹幕");
        }
        this.videoDanmuMapper.insert(bean);
//        this.videoInfoMapper.update(null,new UpdateWrapper<VideoInfo>()
//                .eq("video_id",bean.getVideoId())
//                .setSql("danmu_count = danmu_count+1"));
        videoClient.updateCountInfo(bean.getVideoId(), UserActionTypeEnum.VIDEO_DANMU.getField(),1);

        videoClient.updateDocCount(bean.getVideoId(), SearchOrderTypeEnum.VIDEO_DANMU, 1);

    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class) // 1. 记得加上事务，保证删除和更新计数要么都成功，要么都失败
    public void deleteDanmu(String userId, Integer danmuId) {
        VideoDanmu danmu = videoDanmuMapper.selectById(danmuId);
        if (null == danmu) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
//        VideoInfo videoInfo = videoInfoMapper.selectById(danmu.getVideoId());
        VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(danmu.getVideoId());
        if (null == videoInfo) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 鉴权逻辑：只有视频作者或弹幕发送者（如果是这种逻辑）能删
        if (userId != null && !videoInfo.getUserId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // --- 开始执行删除逻辑 ---

        // 2. 删除弹幕记录
        videoDanmuMapper.deleteById(danmuId);

        // 3. MySQL 视频表计数 -1
//        this.videoInfoMapper.update(null,new UpdateWrapper<VideoInfo>()
//                .eq("video_id",danmu.getVideoId())
//                .setSql("danmu_count = danmu_count-1"));
        videoClient.updateCountInfo(danmu.getVideoId(),UserActionTypeEnum.VIDEO_DANMU.getField(),-1);

        // 4. ES 索引计数 -1
        videoClient.updateDocCount(danmu.getVideoId(), SearchOrderTypeEnum.VIDEO_DANMU, -1);
    }
}
