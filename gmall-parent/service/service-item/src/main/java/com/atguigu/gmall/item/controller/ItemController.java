package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author Administrator
 * @create 2020-05-15 21:40
 */
@Api(tags = "商品详情页接口")
@RequestMapping("/api/item")
@RestController
public class ItemController {

    @Autowired
    private ItemService itemService;

    //获取skuId详情页面信息
    @GetMapping("{skuId}")
    public Result getItem(@PathVariable("skuId") Long skuId){
        Map<String,Object> result = itemService.getItem(skuId);
        return Result.ok(result);
    }
}
