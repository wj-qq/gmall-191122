package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * @author Administrator
 * @create 2020-05-28 16:08
 */
@Controller
public class OrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    //结算  跳往订单页面
    @GetMapping("/trade.html")
    public String trade(Model model){
        //回显数据
        Result<Map<String, Object>> trade = orderFeignClient.trade();
        model.addAllAttributes(trade.getData());
        return "order/trade";
    }




}
