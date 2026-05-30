package com.easylive.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类信息
 */
@Getter
@Setter
public class CategoryInfo implements Serializable {


    /**
     * 自增分类ID
     */
    @TableId(type = IdType.AUTO)
    private Integer categoryId;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 父级分类ID
     */
    @JsonProperty("pCategoryId")
    private Integer pCategoryId;

    /**
     * 图标
     */
    private String icon;

    /**
     * 背景图
     */
    private String background;

    /**
     * 排序号
     */
    private Integer sort;

    @TableField(exist = false)
    public List<CategoryInfo> children = new ArrayList<>();

    public void setChildren(List<CategoryInfo> children) {
        if (children == null) {
            this.children = new ArrayList<>();
        } else {
            this.children = children;
        }
    }


    @Override
    public String toString() {
        return "自增分类ID:" + (categoryId == null ? "空" : categoryId) + "，分类编码:" + (categoryCode == null ? "空" : categoryCode) + "，分类名称:" + (categoryName == null ? "空" : categoryName) + "，父级分类ID:" + (pCategoryId == null ? "空" : pCategoryId) + "，图标:" + (icon == null ? "空" : icon) + "，背景图:" + (background == null ? "空" : background) + "，排序号:" + (sort == null ? "空" : sort);
    }


}
