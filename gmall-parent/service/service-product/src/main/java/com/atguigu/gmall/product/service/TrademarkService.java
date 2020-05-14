package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * @author Administrator
 * @create 2020-05-14 16:19
 */
public interface TrademarkService {
    //品牌分页列表
    IPage<BaseTrademark> trademarkPageList(Integer page, Integer limit);

    //根据id获取品牌信息
    BaseTrademark getTrademark(Long id);

    //添加品牌
    void save(BaseTrademark baseTrademark);

    //跟新品牌
    void update(BaseTrademark baseTrademark);

    //删除品牌
    void remove(Long id);

}
