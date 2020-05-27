package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * 购物车管理
 * @author Administrator
 * @create 2020-05-26 0:45
 */
@Controller
public class CartController {

    @Autowired
    private CartFeignClient cartFeignClient;

    //点击加入购物车 跳转至购物车页面
    @GetMapping("/addCart.html")
    public String addCart(Long skuId, Integer skuNum, Model model){
        CartInfo cartInfo = cartFeignClient.addToCart(skuId, skuNum);
        model.addAttribute("cartInfo",cartInfo);
        return "cart/addCart";
    }

    //去购物车结算
    @GetMapping("/cart.html")
    public String toCart(){
        return "cart/index";
    }

}
