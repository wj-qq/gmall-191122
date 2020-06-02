package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.mq.MqConst;
import com.atguigu.gmall.mq.RabbitService;
import com.atguigu.gmall.payment.mapper.OrderInfoMapper;
import com.atguigu.gmall.payment.mapper.PaymentMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * @author Administrator
 * @create 2020-06-01 21:53
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentMapper paymentMapper;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private RabbitService rabbitService;

    //保存支付信息表
    @Override
    public PaymentInfo savePaymentInfo(Long orderId, PaymentType type) {
        //1:判断支付信息表是否已经保存过了
        PaymentInfo paymentInfo = paymentMapper.selectOne(new QueryWrapper<PaymentInfo>().eq("order_id", orderId));
        if(null == paymentInfo){
            //查询订单信息
            OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
            //如果没有保存过 再保存支付信息表
            paymentInfo = new PaymentInfo();

            paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());//订单中已生成的对外交易编号
            paymentInfo.setOrderId(orderId);//订单编号
            paymentInfo.setPaymentType(type.name());//支付类型（微信与支付宝）
            paymentInfo.setTotalAmount(orderInfo.getTotalAmount());//订单金额
            paymentInfo.setSubject(orderInfo.getTradeBody());//交易内容
            paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());//支付状态，默认值未支付。
            paymentInfo.setCreateTime(new Date());//创建时间，当前时间

            paymentMapper.insert(paymentInfo);
        }


        return paymentInfo;
    }


    //更新支付信息表   四个字段
    @Override
    public void paySuccess(Map<String, String> paramsMap) {
        PaymentInfo paymentInfo = paymentMapper.selectOne(new QueryWrapper<PaymentInfo>().
                eq("out_trade_no", paramsMap.get("out_trade_no")));

        if(PaymentStatus.UNPAID.name().equals(paymentInfo.getPaymentStatus())){
            //更新为已支付
            paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
            //流水号
            paymentInfo.setTradeNo(paramsMap.get("trade_no"));
            //时间
            paymentInfo.setCallbackTime(new Date());
            //将所有Json字符串
            paymentInfo.setCallbackContent(JSONObject.toJSONString(paramsMap));

            paymentMapper.updateById(paymentInfo);

            //更新订单状态  使用MQ
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfo.getOrderId());
        }
    }
}
