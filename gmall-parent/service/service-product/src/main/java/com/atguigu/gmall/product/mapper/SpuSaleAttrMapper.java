package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-05-14 15:35
 */
@Mapper
public interface SpuSaleAttrMapper extends BaseMapper<SpuSaleAttr> {
    List<SpuSaleAttr> spuSaleAttrList(Long spuId);

    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@Param("skuId") Long skuId,@Param("spuId") Long spuId);
}
