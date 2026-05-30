package com.easylive.entity.vo;

import com.easylive.entity.po.UserAction;
import com.easylive.entity.po.VideoComment;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class VideoCommentResultVO {
    private PaginationResultVO<VideoComment> commentData;
    private List<UserAction> userActionList;

}