package com.atguigu.gmall.item.service;

import java.util.Map;

/**
 * @author Administrator
 * @create 2020-05-15 21:41
 */
public interface ItemService {

    //获取skuId详情页面信息
    Map<String, Object> getItem(Long skuId);
}
