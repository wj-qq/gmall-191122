package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-05-26 0:51
 */
public interface CartService {
    CartInfo addToCart(Long skuId, Integer skuNum,String userId);

    List<CartInfo> cartList(String userId, String userTempId);

    void checkCart(Long skuId, Integer isChecked, String userId);
}
