package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Administrator
 * @create 2020-05-26 0:48
 */
@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    @GetMapping("/addToCart/{skuId}/{skuNum}")
    public CartInfo addToCart(@PathVariable("skuId")Long skuId, @PathVariable("skuNum")Integer skuNum, HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        if(StringUtils.isEmpty(userId)){
            userId = AuthContextHolder.getUserTempId(request);
        }
        return cartService.addToCart(skuId,skuNum,userId);
    }
}
