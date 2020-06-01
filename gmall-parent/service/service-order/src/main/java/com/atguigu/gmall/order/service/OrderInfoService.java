package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.order.OrderInfo;

/**
 * @author Administrator
 * @create 2020-05-28 20:01
 */
public interface OrderInfoService {
    boolean hasStock(Long skuId, Integer skuNum);

    Long submitOrder(OrderInfo orderInfo);

    void cancelOrder(Long orderId);

    //du对外暴露订单对象
    OrderInfo getOrderInfo(Long orderId);
}
