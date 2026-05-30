package com.easylive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easylive.api.consumer.WebClient;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.CategoryInfo;
import com.easylive.entity.po.VideoInfo;
import com.easylive.exception.BusinessException;
import com.easylive.mappers.CategoryInfoMapper;
import com.easylive.redis.RedisUtils;
import com.easylive.service.CategoryInfoService;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CategoryInfoServiceImpl extends ServiceImpl<CategoryInfoMapper, CategoryInfo> implements CategoryInfoService {
    @Autowired
    private CategoryInfoMapper categoryInfoMapper;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private WebClient webClient;

    @Override
    public void saveCategory(CategoryInfo categoryInfo) {
        CategoryInfo dbInfo = categoryInfoMapper.selectOne(new LambdaQueryWrapper<CategoryInfo>().eq(CategoryInfo::getCategoryCode, categoryInfo.getCategoryCode()));
        if (dbInfo != null) {
            if (categoryInfo.getCategoryId() == null || !categoryInfo.getCategoryId().equals(dbInfo.getCategoryId())) {
                throw new BusinessException("分类编号已经存在");
            }
        }
        if(categoryInfo.getCategoryId()==null){ // 新增
            // 先检查数据库中是否已经有一样的Code
            CategoryInfo one = categoryInfoMapper.selectOne(new LambdaQueryWrapper<CategoryInfo>()
                    .select(CategoryInfo::getSort)             // 仅查询 sort 字段
                    .eq(CategoryInfo::getPCategoryId, categoryInfo.getPCategoryId())
                    .orderByDesc(CategoryInfo::getSort)        // 按 sort 倒序
                    .last("LIMIT 1"));                         // 只取第一条（最大值）
            categoryInfo.setSort((one == null ? 0 : one.getSort())+1);
            categoryInfoMapper.insert(categoryInfo);
        }
        else{ // 修改
            categoryInfoMapper.updateById(categoryInfo);
        }
        save2Redis();
    }

    @Override
    public void delCategory(Integer categoryId) {
        Integer count = webClient.getVideoCount(categoryId);
        if (count > 0) {
            throw new BusinessException("分类下有视频信息，无法删除");
        }
        remove(new LambdaQueryWrapper<CategoryInfo>().eq(CategoryInfo::getCategoryId,categoryId)
                .or()
                .eq(CategoryInfo::getPCategoryId,categoryId));
        save2Redis();
    }

    @GlobalTransactional
    @Override
    public void changeSort(Integer pCategoryId, String categoryIds) {
        String[] idArray = categoryIds.split(",");
        List<CategoryInfo> updateList = new ArrayList<>();

        for (int i = 0; i < idArray.length; i++) {
            CategoryInfo item = new CategoryInfo();
            item.setCategoryId(Integer.parseInt(idArray[i]));
            item.setPCategoryId(pCategoryId);
            item.setSort(i + 1); // 从1开始排
            updateList.add(item);
        }

        // MP 自带的批量根据 ID 更新
        updateBatchById(updateList);

        save2Redis();
    }

    private void save2Redis() {
        // 1. 使用 MP 的 lambdaQuery 实现全量排序查询
        List<CategoryInfo> allNodes = lambdaQuery()
                .orderByAsc(CategoryInfo::getSort)
                .list();
        List<CategoryInfo> tree = buildTree(allNodes);

        redisUtils.set(Constants.REDIS_KEY_CATEGORY_LIST, tree);
    }

    @Override
    public List<CategoryInfo> buildTree(List<CategoryInfo> allNodes) {
        // 1. 按 PID 分组
        Map<Integer, List<CategoryInfo>> nodeMap = allNodes.stream()
                .collect(Collectors.groupingBy(CategoryInfo::getPCategoryId));

        // 2. 再次遍历设置 children
        allNodes.forEach(node -> node.setChildren(nodeMap.get(node.getCategoryId())));

        // 3. 返回根节点（PID 为 0 的数据）
        return allNodes.stream()
                .filter(n -> Objects.equals(n.getPCategoryId(), 0))
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryInfo> loadAllCategory() {
        List<CategoryInfo> list = (List<CategoryInfo>) redisUtils.get(Constants.REDIS_KEY_CATEGORY_LIST);
        if(list == null || list.isEmpty()){
            save2Redis();
            return (List<CategoryInfo>) redisUtils.get(Constants.REDIS_KEY_CATEGORY_LIST);
        }
        return list;
    }
}
