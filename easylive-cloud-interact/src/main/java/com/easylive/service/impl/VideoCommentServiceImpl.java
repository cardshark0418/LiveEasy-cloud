package com.easylive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easylive.api.consumer.VideoClient;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.po.VideoComment;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.enums.ResponseCodeEnum;
import com.easylive.enums.UserActionTypeEnum;
import com.easylive.exception.BusinessException;

import com.easylive.mapper.VideoCommentMapper;
import com.easylive.service.VideoCommentService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class VideoCommentServiceImpl extends MPJBaseServiceImpl<VideoCommentMapper, VideoComment> implements VideoCommentService {

    @Resource
    private VideoCommentMapper videoCommentMapper;

    @Resource
    private VideoClient videoClient;

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void postComment(VideoComment comment, Integer replyCommentId) {

//        VideoInfo videoInfo = videoInfoMapper.selectById(comment.getVideoId());
        VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(comment.getVideoId());
        if (videoInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //是否关闭评论（"1"=关评论）
        if (isInteractionFlagOn(videoInfo.getInteraction(), "1")) {
            throw new BusinessException("UP主已关闭评论区");
        }
        if (replyCommentId != null) {//如果是回复别人的评论
            VideoComment replyComment = getById(replyCommentId);
            if (replyComment == null || !replyComment.getVideoId().equals(comment.getVideoId())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            if (replyComment.getPCommentId() == 0) {//如果回复的评论是回复视频的评论
                comment.setPCommentId(replyComment.getCommentId());
            } else {//如果是视频的回复的回复
                comment.setPCommentId(replyComment.getPCommentId());
                comment.setReplyUserId(replyComment.getUserId());
            }
//            UserInfo userInfo = userInfoMapper.selectById(replyComment.getUserId());
            UserInfo userInfo = videoClient.getUserInfoByUserId(replyComment.getUserId());
            comment.setReplyNickName(userInfo.getNickName());
            comment.setReplyAvatar(userInfo.getAvatar());
        } else {//如果是回复视频
            comment.setPCommentId(0);
        }
        comment.setPostTime(new Date());
        comment.setVideoUserId(videoInfo.getUserId());
        this.videoCommentMapper.insert(comment);


        //增加评论数
        if (comment.getPCommentId() == 0) {
            videoClient.updateCountInfo(comment.getVideoId(),UserActionTypeEnum.VIDEO_COMMENT.getField(),1);
        }
    }

    @Override
    public PaginationResultVO<VideoComment> getCommentList(String videoId, Integer pageNo, Integer orderType) {
        Page<VideoComment> page = new Page<>(pageNo == null ? 1 : pageNo, 15);

        // 创建 MPJLambdaWrapper
        MPJLambdaWrapper<VideoComment> wrapper = new MPJLambdaWrapper<VideoComment>()
                .selectAll(VideoComment.class) // 查询父评论
                // 关键：使用 selectCollection 映射子评论列表
                .selectCollection(VideoComment.class, VideoComment::getChildren, collection -> collection
                                .result(VideoComment::getCommentId)
                                .result(VideoComment::getContent)
                                .result(VideoComment::getLikeCount)
                                .result(VideoComment::getPostTime)
                        // 绑定映射关系：子评论的 pCommentId 等于父评论的 commentId
                        // 注意：这里通常在 mapper 层面通过 leftJoin 关联，或者使用 .map()
                )
                // 必须显式地进行左连接，并给子表起一个别名（例如 "sub"）
                .leftJoin(VideoComment.class, "sub", VideoComment::getPCommentId, VideoComment::getCommentId)
                .eq(VideoComment::getVideoId, videoId)
                .eq(VideoComment::getPCommentId, 0); // 主表只查一级评论

        // 排序
        if (orderType == null || orderType == 0) {
            wrapper.orderByDesc(VideoComment::getLikeCount).orderByDesc(VideoComment::getCommentId);
        } else {
            wrapper.orderByDesc(VideoComment::getCommentId);
        }

        // 执行分页
        IPage<VideoComment> resultPage = videoCommentMapper.selectJoinPage(page, VideoComment.class, wrapper);

        return new PaginationResultVO<>(
                (int) resultPage.getTotal(),
                (int) resultPage.getSize(),
                (int) resultPage.getCurrent(),
                (int) resultPage.getPages(),
                resultPage.getRecords()
        );
    }
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void deleteComment(Integer commentId, String userId) {
        VideoComment comment = videoCommentMapper.selectById(commentId);
        if (null == comment) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
//        VideoInfo videoInfo = videoInfoMapper.selectById(comment.getVideoId());
        VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(comment.getVideoId());
        if (null == videoInfo) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (userId != null && !videoInfo.getUserId().equals(userId) && !comment.getUserId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        videoCommentMapper.deleteById(commentId);
        if (comment.getPCommentId() == 0) {
//            videoInfoMapper.update(null,new LambdaUpdateWrapper<VideoInfo>()
//                    .eq(VideoInfo::getVideoId,videoInfo.getVideoId())
//                    .setSql("comment_count = comment_count - 1"));
            videoClient.updateCountInfo(videoInfo.getVideoId(),UserActionTypeEnum.VIDEO_COMMENT.getField(),-1);

            //删除二级评论
            videoCommentMapper.delete(new LambdaQueryWrapper<VideoComment>()
                    .eq(VideoComment::getPCommentId,commentId));
        }
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void topComment(Integer commentId, String userId) {
        VideoComment comment = videoCommentMapper.selectById(commentId);
        if (comment == null || comment.getPCommentId() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(comment.getVideoId());
        if (videoInfo == null || !videoInfo.getUserId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        videoCommentMapper.update(null, new LambdaUpdateWrapper<VideoComment>()
                .eq(VideoComment::getVideoId, comment.getVideoId())
                .eq(VideoComment::getTopType, 1)
                .set(VideoComment::getTopType, 0));
        videoCommentMapper.update(null, new LambdaUpdateWrapper<VideoComment>()
                .eq(VideoComment::getCommentId, commentId)
                .set(VideoComment::getTopType, 1));
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void cancelTopComment(Integer commentId, String userId) {
        VideoComment comment = videoCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(comment.getVideoId());
        if (videoInfo == null || !videoInfo.getUserId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        videoCommentMapper.update(null, new LambdaUpdateWrapper<VideoComment>()
                .eq(VideoComment::getCommentId, commentId)
                .set(VideoComment::getTopType, 0));
    }

    /** interaction 为逗号分隔标记："0"=关弹幕，"1"=关评论 */
    private boolean isInteractionFlagOn(String interaction, String flag) {
        if (interaction == null || interaction.isEmpty()) {
            return false;
        }
        for (String part : interaction.split(",")) {
            if (flag.equals(part.trim())) {
                return true;
            }
        }
        return false;
    }
}