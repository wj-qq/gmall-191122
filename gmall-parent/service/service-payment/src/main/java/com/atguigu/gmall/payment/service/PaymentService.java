package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

/**
 * @author Administrator
 * @create 2020-06-01 21:53
 */
public interface PaymentService {

    //保存支付信息表
    PaymentInfo savePaymentInfo(Long orderId, PaymentType alipay);

    //更新支付信息表   四个字段
    void paySuccess(Map<String, String> paramsMap);
}
