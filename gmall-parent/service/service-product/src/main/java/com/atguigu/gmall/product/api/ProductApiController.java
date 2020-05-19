package com.atguigu.gmall.product.api;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @create 2020-05-15 19:36
 */
@Api(tags = "前端商品api接口")
@RestController
@RequestMapping("/api/product")
public class ProductApiController {

    @Autowired
    private ManageService manageService;

    //根据skuId获取sku信息
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable("skuId")Long skuId){
        return manageService.getSkuInfo(skuId);
    }

    //通过三级分类id查询分类信息
    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable("category3Id") Long category3Id){
        return manageService.getCategoryView(category3Id);
    }

    //获取sku价格
    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable("skuId")Long skuId){
        return manageService.getSkuPrice(skuId);
    }

    //根据spuId，skuId 查询销售属性集合
    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable("skuId")Long skuId, @PathVariable("spuId")Long spuId){
        return manageService.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    //根据spuId 查询map 集合属性  销售属性值的组合
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable("spuId")Long spuId){
        return manageService.getSkuValueIdsMap(spuId);
    }


    //获取全部分类信息  首页使用
    @GetMapping("getBaseCategoryList")
    public List<Map> getBaseCategoryList(){
        return manageService.getBaseCategoryList();
    }
}
