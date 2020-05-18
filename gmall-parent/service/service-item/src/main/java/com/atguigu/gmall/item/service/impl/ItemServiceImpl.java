package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.config.ThreadPoolConfig;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Administrator
 * @create 2020-05-15 21:41
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    //获取skuId详情页面信息

    @Override
    public Map<String, Object> getItem(Long skuId) {
        //使用异步编排  多线程优化
        HashMap<String, Object> map = new HashMap<>();

        //根据skuId获取sku信息
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            map.put("skuInfo", skuInfo);
            return skuInfo;
        }, threadPoolExecutor);

        //通过三级分类id查询分类信息  与查询skuInfo信息串行
        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            map.put("categoryView", categoryView);
        }, threadPoolExecutor);


        //获取sku价格  与查询skuInfo信息并行
        CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal price = productFeignClient.getSkuPrice(skuId);
            map.put("price", price);
        }, threadPoolExecutor);


        //根据spuId，skuId 查询销售属性集合
        CompletableFuture<Void> spuSaleAttrListCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            List<SpuSaleAttr> spuSaleAttrListCheckBySkuList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            map.put("spuSaleAttrList", spuSaleAttrListCheckBySkuList);
        }, threadPoolExecutor);


        //根据spuId 查询map 集合属性  销售属性值的组合
        CompletableFuture<Void> valuesSkuJsonCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            map.put("valuesSkuJson", JSON.toJSONString(skuValueIdsMap));
        }, threadPoolExecutor);

        //选择：要不要等待上面的线程执行完成  选择不等  也可以选择等待
        //本次是必须要等待上面多线程全部执行完成
        CompletableFuture.allOf(skuInfoCompletableFuture,categoryViewCompletableFuture,priceCompletableFuture,
                spuSaleAttrListCompletableFuture,valuesSkuJsonCompletableFuture ).join();
        return map;
    }
}
