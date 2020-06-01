package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import net.bytebuddy.asm.Advice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝管理
 * @author Administrator
 * @create 2020-06-01 21:54
 */
@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private AlipayClient alipayClient;

    //支付宝支付  调用支付宝接口
    @Override
    public String submit(Long orderId) {
        //保存支付信息表
        PaymentInfo paymentInfo = paymentService.savePaymentInfo(orderId, PaymentType.ALIPAY);
        //调用支付宝接口  统一收单下单并支付页面接口
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        Map map = new HashMap<>();
        map.put("out_trade_no", paymentInfo.getOutTradeNo());
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        map.put("total_amount", paymentInfo.getTotalAmount());
        map.put("subject", paymentInfo.getSubject());
        request.setBizContent(JSONObject.toJSONString(map));

        //设置消费者查看跳转页面  同步刷新页面   商家成功页面
        request.setReturnUrl(AlipayConfig.return_payment_url);
        //设置异步通知商家
        request.setNotifyUrl(AlipayConfig.notify_payment_url);

        AlipayTradePagePayResponse response = null;
        try {
            response = alipayClient.pageExecute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        System.out.println(response.getBody());

        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }

        return response.getBody();
    }

    //退钱
    @Override
    public void refund(String outTradeNo) {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        Map map = new HashMap<>();
        map.put("out_trade_no", outTradeNo);
        map.put("refund_amount", "31791.00");
        request.setBizContent(JSONObject.toJSONString(map));
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
    }
}
