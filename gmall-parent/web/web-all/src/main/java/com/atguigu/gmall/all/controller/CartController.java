package com.atguigu.gmall.all.controller;

import org.springframework.http.HttpStatus;
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

    //点击加入购物车 跳转至购物车页面
    @GetMapping("/addCart.html")
    public String addCart(Long skuId, Integer skuNum, Model model, HttpServletRequest request){
        request.getHeader("");

        return "cart/addCart";
    }

}
