package com.easylive.controller;

import com.easylive.api.consumer.CategoryClient;
import com.easylive.entity.vo.ResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@RequestMapping("/category")
@Validated
public class CategoryController {

    @Autowired
    private CategoryClient categoryClient;

    @RequestMapping("/loadAllCategory")
    public ResponseVO loadAllCategory() {
        return getSuccessResponseVO(categoryClient.loadAllCategory());
    }


}

