package com.atguigu.gmall.payment.service;

/**
 * @author Administrator
 * @create 2020-06-01 21:54
 */
public interface AlipayService {

    //支付宝支付
    String submit(Long orderId);

    //退钱
    void refund(String outTradeNo);
}
