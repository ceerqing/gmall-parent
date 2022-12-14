package com.atguigu.gmall.product.service;


import com.atguigu.gmall.model.product.BaseCategory3;
import com.atguigu.gmall.model.to.CategoryViewTo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 张世平哒
* @description 针对表【base_category3(三级分类表)】的数据库操作Service
* @createDate 2022-08-26 09:28:02
*/

public interface BaseCategory3Service extends IService<BaseCategory3> {
    /**
     * 根据二级分类id获取三级分类
     * @param id
     * @return
     */
    List<BaseCategory3> getCategory3Chile(Long id);

    CategoryViewTo  getCategoryView(Long c3Id);
}
