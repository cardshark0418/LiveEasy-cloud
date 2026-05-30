package com.easylive.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.mapper.VideoInfoFilePostMapper;
import com.easylive.service.VideoInfoFilePostService;
import org.springframework.stereotype.Service;

@Service
public class VideoInfoFilePostServiceImpl extends ServiceImpl<VideoInfoFilePostMapper, VideoInfoFilePost> implements VideoInfoFilePostService {
}
