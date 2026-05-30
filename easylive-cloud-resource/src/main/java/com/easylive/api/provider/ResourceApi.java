package com.easylive.api.provider;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.easylive.controller.FileController;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.UserInfo;
import feign.Response;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


@RestController
@Validated
@RequestMapping(Constants.INNER_API_PREFIX + "/file")
public class ResourceApi {


    @Resource
    private FileController fileController;

    @RequestMapping("/getResource")
    void getResource(HttpServletResponse response, @RequestParam String sourceName){
        fileController.getResource(response,sourceName);
    }

}