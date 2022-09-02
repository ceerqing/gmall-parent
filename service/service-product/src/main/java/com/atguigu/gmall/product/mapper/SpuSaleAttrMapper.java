package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.model.to.ValueSkuJsonTo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author 张世平哒
* @description 针对表【spu_sale_attr(spu销售属性)】的数据库操作Mapper
* @createDate 2022-08-27 09:53:41
* @Entity com.atguigu.gmall.product.domain.SpuSaleAttr
*/
public interface SpuSaleAttrMapper extends BaseMapper<SpuSaleAttr> {

    List<SpuSaleAttr> getAttrAndValueBySpuId(@Param("spuId") Long supId);

    List<SpuSaleAttr> getSaleAttrAndValueMarkSku(@Param("spuId") Long spuId,@Param("skuId") Long skuId);

    List<ValueSkuJsonTo> getSupAllSukIdAndValueId(@Param("spuId") Long spuId);
}




