package com.easylive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easylive.entity.po.CategoryInfo;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.CategoryInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

import static com.easylive.entity.vo.ResponseVO.getSuccessResponseVO;

@RestController
@RequestMapping("/category")
@Validated
public class CategoryController {

    @Autowired
    private CategoryInfoService categoryInfoService;

    @RequestMapping("/loadCategory")
    public ResponseVO loadCategory(CategoryInfo categoryInfo) {
        LambdaQueryWrapper<CategoryInfo> wrapper = new LambdaQueryWrapper<>(categoryInfo);
        wrapper.orderByAsc(CategoryInfo::getSort);
        List<CategoryInfo> list = categoryInfoService.list(wrapper);

        return getSuccessResponseVO(categoryInfoService.buildTree(list));
    }

    /*
     即可新增也可修改
     */
    @RequestMapping("/saveCategory")
    public ResponseVO saveCategory(@NotNull Integer pCategoryId,
                                   Integer categoryId,
                                   @NotEmpty String categoryCode,
                                   @NotEmpty String categoryName,
                                   String icon,
                                   String background) {
        CategoryInfo categoryInfo = new CategoryInfo();
        categoryInfo.setPCategoryId(pCategoryId);
        categoryInfo.setCategoryId(categoryId);
        categoryInfo.setCategoryCode(categoryCode);
        categoryInfo.setCategoryName(categoryName);
        categoryInfo.setIcon(icon);
        categoryInfo.setBackground(background);
        categoryInfoService.saveCategory(categoryInfo);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/delCategory")
    public ResponseVO delCategory(@NotNull Integer categoryId) {
        categoryInfoService.delCategory(categoryId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/changeSort")
    public ResponseVO changeSort(@NotNull Integer pCategoryId,
                                 @NotEmpty String categoryIds) {
        categoryInfoService.changeSort(pCategoryId, categoryIds);
        return getSuccessResponseVO(null);
    }
}

