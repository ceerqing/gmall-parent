package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.SkuImage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 张世平哒
* @description 针对表【sku_image(库存单元图片表)】的数据库操作Service
* @createDate 2022-08-27 09:53:41
*/
public interface SkuImageService extends IService<SkuImage> {

    /**
     * 根据skuId获取一个sku的所有图片
     * @param skuId
     * @return
     */
    List<SkuImage> getSkuImage(Long skuId);
}
