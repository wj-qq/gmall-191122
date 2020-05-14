package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.TrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Administrator
 * @create 2020-05-14 16:19
 */
@Service
public class TrademarkServiceImpl implements TrademarkService {

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    //品牌分页列表
    @Override
    public IPage<BaseTrademark> trademarkPageList(Integer page, Integer limit) {
        return baseTrademarkMapper.selectPage(new Page<BaseTrademark>(page,limit),null);
    }

    //根据id获取品牌信息
    @Override
    public BaseTrademark getTrademark(Long id) {
        return baseTrademarkMapper.selectById(id);
    }

    //添加品牌
    @Override
    public void save(BaseTrademark baseTrademark) {
        baseTrademarkMapper.insert(baseTrademark);
    }

    //跟新品牌
    @Override
    public void update(BaseTrademark baseTrademark) {
        baseTrademarkMapper.updateById(baseTrademark);
    }

    //删除品牌
    @Override
    public void remove(Long id) {
        baseTrademarkMapper.deleteById(id);
    }
}
