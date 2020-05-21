package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.BaseAttrValue;
import com.atguigu.gmall.model.product.SkuAttrValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-05-14 20:17
 */
@Mapper
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValue> {

    List<SkuAttrValue> getAttrList(Long skuId);
}
