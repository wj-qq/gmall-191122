package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-05-26 0:51
 */
public interface CartService {

    //添加购物车
    CartInfo addToCart(Long skuId, Integer skuNum,String userId);

    //去购物车结算
    List<CartInfo> cartList(String userId, String userTempId);

    //购物车的 选中或取消
    void checkCart(Long skuId, Integer isChecked, String userId);

    //删除购物车
    void deleteCart(Long skuId, String userId);

    //结算 获取购买商品集合
    List<CartInfo> getCartCheckedList(Long userId);
}
