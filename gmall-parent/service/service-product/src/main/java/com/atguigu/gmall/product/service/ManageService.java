package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @create 2020-05-12 22:56
 */
public interface ManageService {
    //获取商品一级分类
    List<BaseCategory1> getCategory1();

    //获取商品二级分类
    List<BaseCategory2> getCategory2(Long category1Id);

    //获取商品三级分类
    List<BaseCategory3> getCategory3(Long category2Id);

    //根据分类id获取平台属性
    List<BaseAttrInfo> attrInfoList(Long category1Id, Long category2Id, Long category3Id);

    //添加平台属性//修改平台属性
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    //根据平台属性ID获取平台属性
    List<BaseAttrValue> getAttrValueList(Long attrId);

    //获取销售属性
    List<BaseSaleAttr> baseSaleAttrList();

    //获取spu分页列表
    IPage<SpuInfo> spuPageList(Integer page, Integer limit, Long category3Id);

    //获取品牌属性
    List<BaseTrademark> getTrademarkList();

    //添加spu
    void saveSpuInfo(SpuInfo spuInfo);

    //根据spuId获取图片列表
    List<SpuImage> spuImageList(Long spuId);

    //根据spuId获取销售属性
    List<SpuSaleAttr> spuSaleAttrList(Long spuId);

    //添加sku
    void saveSkuInfo(SkuInfo skuInfo);

    //获取sku分页列表
    IPage<SkuInfo> skuPageList(Integer page, Integer limit);

    //上架商品
    void onSale(Long skuId);

    //下架商品
    void cancelSale(Long skuId);

    //根据skuId获取sku信息
    SkuInfo getSkuInfo(Long skuId);

    //通过三级分类id查询分类信息
    BaseCategoryView getCategoryView(Long category3Id);

    //获取sku价格
    BigDecimal getSkuPrice(Long skuId);

    //根据spuId，skuId 查询销售属性集合
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    //根据spuId 查询map 集合属性  销售属性值的组合
    Map getSkuValueIdsMap(Long spuId);

    //获取全部分类信息  首页使用
    List<Map> getBaseCategoryList();

    //根据tmId 查询品牌信息
    BaseTrademark getBaseTrademark(Long tmId);

    //根据skuId 查询平台属性值集合
    List<SkuAttrValue> getAttrList(Long skuId);

}
