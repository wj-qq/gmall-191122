package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

/**
 * @author Administrator
 * @create 2020-05-21 16:00
 */
public interface ListService {

    //上架商品  保存索引库
    void upperGoods(Long skuId);

    //下架商品  删除索引库
    void lowerGoods(Long skuId);

    //更新商品incrHotScore
    void incrHotScore(Long skuId);

    //搜索
    SearchResponseVo list(SearchParam searchParam);
}
