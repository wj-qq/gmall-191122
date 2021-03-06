package com.atguigu.gmall.product.client;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.impl.ProductDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @create 2020-05-15 21:34
 */
@FeignClient(value = "service-product", fallback = ProductDegradeFeignClient.class)
public interface ProductFeignClient {

    //根据skuId获取sku信息
    @GetMapping("/api/product/inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable("skuId")Long skuId);

    //通过三级分类id查询分类信息
    @GetMapping("/api/product/inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable("category3Id") Long category3Id);

    //获取sku价格
    @GetMapping("/api/product/inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable("skuId")Long skuId);

    //根据spuId，skuId 查询销售属性集合
    @GetMapping("/api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId")Long skuId, @PathVariable("spuId")Long spuId);

    //根据spuId 查询map 集合属性  销售属性值的组合
    @GetMapping("/api/product/inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable("spuId")Long spuId);

    //获取全部分类信息  首页使用
    @GetMapping("/api/product/getBaseCategoryList")
    public List<Map> getBaseCategoryList();

    //根据tmId 查询品牌信息
    @GetMapping("/api/product/getBaseTrademark/{tmId}")
    public BaseTrademark getBaseTrademark(@PathVariable("tmId")Long tmId);

    //根据skuId 查询平台属性值集合
    @GetMapping("/api/product/inner/getAttrList/{skuId}")
    public List<SkuAttrValue> getAttrList(@PathVariable("skuId")Long skuId);

}
