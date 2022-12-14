package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.SkuImage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.product.service.SkuImageService;
import com.atguigu.gmall.product.mapper.SkuImageMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
* @author 张世平哒
* @description 针对表【sku_image(库存单元图片表)】的数据库操作Service实现
* @createDate 2022-08-27 09:53:41
*/
@Service
public class SkuImageServiceImpl extends ServiceImpl<SkuImageMapper, SkuImage>
    implements SkuImageService{

    @Resource
    SkuImageMapper skuImageMapper;
    @Override
    public List<SkuImage> getSkuImage(Long skuId) {
        QueryWrapper<SkuImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sku_id",skuId);
        return skuImageMapper.selectList(queryWrapper);
    }
}




