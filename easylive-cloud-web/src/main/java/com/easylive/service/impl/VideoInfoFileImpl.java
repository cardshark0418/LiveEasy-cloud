package com.easylive.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easylive.entity.po.VideoInfoFile;
import com.easylive.mapper.VideoInfoFileMapper;
import com.easylive.service.VideoInfoFileService;
import org.springframework.stereotype.Service;

@Service
public class VideoInfoFileImpl extends ServiceImpl<VideoInfoFileMapper, VideoInfoFile> implements VideoInfoFileService {
}
