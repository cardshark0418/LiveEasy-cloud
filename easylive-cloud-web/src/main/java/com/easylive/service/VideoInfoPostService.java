package com.easylive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.query.VideoInfoPostQuery;
import com.easylive.entity.vo.PaginationResultVO;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 视频信息 业务接口
 */
public interface VideoInfoPostService extends IService<VideoInfoPost> {

    void saveVideoInfo(VideoInfoPost videoInfo, List<VideoInfoFilePost> fileInfoList);

    @Transactional(rollbackFor = Exception.class)
    void transferVideoFile(VideoInfoFilePost videoInfoFile);

    PaginationResultVO findListByPage(VideoInfoPostQuery videoInfoQuery);

    void auditVideo(@NotEmpty String videoId, @NotNull Integer status, String reason);

    void recommendVideo(@NotEmpty String videoId);
}