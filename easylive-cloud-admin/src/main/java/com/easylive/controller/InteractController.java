package com.easylive.controller;

import com.easylive.api.consumer.InteractClient;
import com.easylive.entity.vo.ResponseVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@RequestMapping("/interact")
@Validated
public class InteractController {

    @Resource
    private InteractClient interactClient;


    @RequestMapping("/loadDanmu")
    public ResponseVO loadDanmu(@RequestParam(required = false) Integer pageNo, @RequestParam(required = false) String videoNameFuzzy) {
        return getSuccessResponseVO(interactClient.loadDanmu(pageNo,videoNameFuzzy));
    }


    @RequestMapping("/delDanmu")
    public ResponseVO delDanmu(@NotNull Integer danmuId) {
        interactClient.delDanmu(danmuId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadComment")
    public ResponseVO loadComment(@RequestParam(required = false) Integer pageNo, @RequestParam(required = false) String videoNameFuzzy) {
        return getSuccessResponseVO(interactClient.loadComment(pageNo,videoNameFuzzy));

    }

    @RequestMapping("/delComment")
    public ResponseVO delComment(@NotNull Integer commentId) {
        interactClient.delComment(commentId);
        return getSuccessResponseVO(null);
    }
}