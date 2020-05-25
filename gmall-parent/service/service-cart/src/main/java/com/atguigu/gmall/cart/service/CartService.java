package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

/**
 * @author Administrator
 * @create 2020-05-26 0:51
 */
public interface CartService {
    CartInfo addToCart(Long skuId, Integer skuNum,String userId);
}
