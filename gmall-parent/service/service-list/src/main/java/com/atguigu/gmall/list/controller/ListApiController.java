package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.ListService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * @author Administrator
 * @create 2020-05-19 22:01
 */
@RestController
@RequestMapping("/api/list")
public class ListApiController {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;//高版本
    @Autowired
    private ListService listService;


    //索引库Mappings映射
    @GetMapping("/index")
    public Result index(){
        //创建索引库
        elasticsearchRestTemplate.createIndex(Goods.class);
        //Mappings映射 域及类型
        elasticsearchRestTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    //上架商品  保存索引库
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable("skuId")Long skuId){
        listService.upperGoods(skuId);
        return Result.ok();
    }

    //下架商品  删除索引库
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable("skuId")Long skuId){
        listService.lowerGoods(skuId);
        return Result.ok();
    }

    //更新商品incrHotScore
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable("skuId")Long skuId){
        listService.incrHotScore(skuId);
        return Result.ok();
    }

    //搜索
    @PostMapping
    public SearchResponseVo list(@RequestBody SearchParam searchParam){
        return listService.list(searchParam);
    }

}
