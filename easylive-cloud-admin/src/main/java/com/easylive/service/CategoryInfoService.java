package com.easylive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.easylive.entity.po.CategoryInfo;


import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 分类信息 业务接口
 */
public interface CategoryInfoService extends IService<CategoryInfo> {


    void saveCategory(CategoryInfo categoryInfo);

    void delCategory(@NotNull Integer categoryId);

    void changeSort(@NotNull Integer pCategoryId, @NotEmpty String categoryIds);

    Object buildTree(List<CategoryInfo> list);

    List<CategoryInfo> loadAllCategory();
}