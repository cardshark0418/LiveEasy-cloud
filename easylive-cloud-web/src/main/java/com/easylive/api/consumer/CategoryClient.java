package com.easylive.api.consumer;

import com.easylive.entity.po.CategoryInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient(name = "easylive-cloud-admin", contextId = "categoryClient")
public interface CategoryClient {

    @RequestMapping("/innerApi/loadAllCategory")
    List<CategoryInfo> loadAllCategory();
}