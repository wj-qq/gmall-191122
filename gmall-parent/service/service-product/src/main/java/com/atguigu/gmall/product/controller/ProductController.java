package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品后台管理控制层
 * @author Administrator
 * @create 2020-05-12 22:40
 */
@RestController
@RequestMapping("/admin/product")
@Api(tags = "商品管理接口")
public class ProductController {
    @Autowired
    private ManageService manageService;
    @Autowired

    //获取商品一级分类
    @ApiOperation("获取商品一级分类")
    @GetMapping("/getCategory1")
    public Result getCategory1(){
        List<BaseCategory1> baseCategory1List = manageService.getCategory1();
        return Result.ok(baseCategory1List);
    }

    //获取商品二级分类
    @GetMapping("/getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable("category1Id") Long category1Id){
        List<BaseCategory2> baseCategory2List = manageService.getCategory2(category1Id);
        return Result.ok(baseCategory2List);
    }

    //获取商品三级分类
    @GetMapping("/getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable("category2Id") Long category2Id){
        List<BaseCategory3> baseCategory3List = manageService.getCategory3(category2Id);
        return Result.ok(baseCategory3List);
    }

    //根据分类id获取平台属性
    @GetMapping("/attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result attrInfoList(
            @PathVariable("category1Id") Long category1Id,
            @PathVariable("category2Id") Long category2Id,
            @PathVariable("category3Id") Long category3Id ){
        List<BaseAttrInfo> baseAttrInfoList = manageService.attrInfoList(category1Id,category2Id,category3Id);
        return Result.ok(baseAttrInfoList);
    }

    //添加平台属性//修改平台属性
    @PostMapping("/saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    //根据平台属性ID获取平台属性
    @GetMapping("/getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable("attrId") Long attrId){
        List<BaseAttrValue> baseAttrValueList = manageService.getAttrValueList(attrId);
        return Result.ok(baseAttrValueList);
    }

    //获取销售属性
    @GetMapping("/baseSaleAttrList")
    public Result baseSaleAttrList(){
        List<BaseSaleAttr> baseSaleAttrList = manageService.baseSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }

    //获取spu分页列表
    @GetMapping("/{page}/{limit}")
    public Result spuPageList(@PathVariable("page")Integer page,@PathVariable("limit")Integer limit,Long category3Id){
        IPage<SpuInfo> spuInfoIPage = manageService.spuPageList(page, limit, category3Id);
        return Result.ok(spuInfoIPage);
    }

    //获取品牌属性
    @GetMapping("/baseTrademark/getTrademarkList")
    public Result getTrademarkList(){
        List<BaseTrademark> baseTrademarkList = manageService.getTrademarkList();
        return Result.ok(baseTrademarkList);
    }

    //添加spu
    @PostMapping("/saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }

    //根据spuId获取图片列表
    @GetMapping("/spuImageList/{spuId}")
    public Result spuImageList(@PathVariable("spuId")Long spuId){
        List<SpuImage> spuImageList = manageService.spuImageList(spuId);
        return Result.ok(spuImageList);
    }

    //根据spuId获取销售属性
    @GetMapping("/spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable("spuId") Long spuId){
        List<SpuSaleAttr> spuSaleAttrList = manageService.spuSaleAttrList(spuId);
        return Result.ok(spuSaleAttrList);
    }

    //添加sku
    @PostMapping("/saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    //获取sku分页列表
    @GetMapping("/list/{page}/{limit}")
    public Result skuPageList(@PathVariable("page")Integer page,@PathVariable("limit")Integer limit){
        IPage<SkuInfo> skuInfoIPage = manageService.skuPageList(page,limit);
        return Result.ok(skuInfoIPage);
    }

    //上架商品
    @GetMapping("/onSale/{skuId}")
    public Result onSale(@PathVariable("skuId")Long skuId){
        manageService.onSale(skuId);
        return Result.ok();
    }

    //下架商品
    @GetMapping("/cancelSale/{skuId}")
    public Result cancelSale(@PathVariable("skuId")Long skuId){
        manageService.cancelSale(skuId);
        return Result.ok();
    }
}
