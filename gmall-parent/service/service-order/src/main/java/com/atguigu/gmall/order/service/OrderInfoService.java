package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;

import java.util.List;
import java.util.Map;

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

    void updateOrderStatus(Long orderId, OrderStatus paid, ProcessStatus paid1);

    void updateOrderStatus(Long orderId,ProcessStatus paid1);

    public Map<String,Object> initWareOrder(OrderInfo orderInfo);

    //发消息 到库存微服务 扣减库存
    void sendOrderStatus(Long orderId);

    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);
}
