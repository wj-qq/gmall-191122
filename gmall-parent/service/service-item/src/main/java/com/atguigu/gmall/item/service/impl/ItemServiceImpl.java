package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @create 2020-05-15 21:41
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    //获取skuId详情页面信息
    @Override
    public Map<String, Object> getItem(Long skuId) {
        HashMap<String, Object> map = new HashMap<>();
        //根据skuId获取sku信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        //通过三级分类id查询分类信息
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        //获取sku价格
        BigDecimal price = productFeignClient.getSkuPrice(skuId);
        //根据spuId，skuId 查询销售属性集合
        List<SpuSaleAttr> spuSaleAttrListCheckBySkuList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
        //根据spuId 查询map 集合属性  销售属性值的组合
        Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());

        map.put("skuInfo",skuInfo);
        map.put("categoryView",categoryView);
        map.put("price",price);
        map.put("spuSaleAttrList",spuSaleAttrListCheckBySkuList);
        map.put("valuesSkuJson", JSON.toJSONString(skuValueIdsMap));

        return map;
    }
}
