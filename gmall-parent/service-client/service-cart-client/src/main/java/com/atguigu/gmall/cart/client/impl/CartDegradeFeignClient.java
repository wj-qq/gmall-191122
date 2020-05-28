package com.atguigu.gmall.cart.client.impl;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-05-26 20:29
 */
@Component
public class CartDegradeFeignClient implements CartFeignClient {
    @Override
    public CartInfo addToCart(Long skuId, Integer skuNum) {
        return null;
    }

    @Override
    public List<CartInfo> getCartCheckedList(Long userId) {
        return null;
    }
}
