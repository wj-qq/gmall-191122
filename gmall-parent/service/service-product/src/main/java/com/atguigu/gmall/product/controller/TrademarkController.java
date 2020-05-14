package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.TrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Administrator
 * @create 2020-05-14 16:15
 */
@RestController
@RequestMapping("/admin/product/baseTrademark")
@Api(tags = "品牌管理接口")
public class TrademarkController {
    @Autowired
    private TrademarkService trademarkService;

    //品牌分页列表
    @GetMapping("/{page}/{limit}")
    public Result trademarkPageList(@PathVariable("page")Integer page, @PathVariable("limit")Integer limit){
       IPage<BaseTrademark> baseTrademarkIPage = trademarkService.trademarkPageList(page,limit);
        return Result.ok(baseTrademarkIPage);
    }

    //根据id获取品牌信息
    @GetMapping("/get/{id}")
    public Result getTrademark(@PathVariable("id")Long id){
        BaseTrademark baseTrademark = trademarkService.getTrademark(id);
        return Result.ok(baseTrademark);
    }

    //添加品牌
    @PostMapping("/save")
    public Result save(@RequestBody BaseTrademark baseTrademark){
        trademarkService.save(baseTrademark);
        return Result.ok();
    }

    //跟新品牌
    @PutMapping("/update")
    public Result update(@RequestBody BaseTrademark baseTrademark){
        trademarkService.update(baseTrademark);
        return Result.ok();
    }

    //删除品牌
    @DeleteMapping("/remove/{id}")
    public Result remove(@PathVariable("id") Long id){
        trademarkService.remove(id);
        return Result.ok();
    }
}
