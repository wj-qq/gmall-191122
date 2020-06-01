package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 跳转支付页面
 * @author Administrator
 * @create 2020-06-01 19:58
 */
@Controller
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    //跳至支付页面
    @GetMapping("/pay.html")
    public String toPay(Long orderId, Model model){
        //远程调用 订单信息
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        model.addAttribute("orderInfo",orderInfo);

        return "payment/pay";
    }


    //转发成功页面
    @GetMapping("/pay/success.html")
    public String success(){
        return "payment/success";
    }
}
