package com.easylive.api.provider;


import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.CategoryInfo;
import com.easylive.service.CategoryInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX)
public class CategoryApi {

    @Autowired
    private CategoryInfoService categoryInfoService;

    @RequestMapping("/loadAllCategory")
    public List<CategoryInfo> loadAllCategory(){
        return categoryInfoService.loadAllCategory();
    }
}
