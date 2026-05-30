package com.easylive.service;

import com.easylive.entity.po.VideoComment;
import com.easylive.entity.vo.PaginationResultVO;
import com.github.yulichang.base.MPJBaseService;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public interface VideoCommentService extends MPJBaseService<VideoComment> {
    void postComment(VideoComment comment, Integer replyCommentId);

    PaginationResultVO<VideoComment> getCommentList(@NotEmpty String videoId, Integer pageNo, Integer orderType);

    void deleteComment(@NotNull Integer commentId, String userId);
}