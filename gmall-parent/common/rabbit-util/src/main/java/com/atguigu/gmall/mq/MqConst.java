package com.atguigu.gmall.mq;

/**
 * @author Administrator
 * @create 2020-06-01 15:57
 */
public class MqConst {

    /**
     * 商品上下架
     */
    public static final String EXCHANGE_DIRECT_GOODS = "exchange.direct.goods";
    public static final String ROUTING_GOODS_UPPER = "goods.upper";
    public static final String ROUTING_GOODS_LOWER = "goods.lower";
    //队列
    public static final String QUEUE_GOODS_UPPER  = "queue.goods.upper";
    public static final String QUEUE_GOODS_LOWER  = "queue.goods.lower";


    /**
     * 取消订单，发送延迟队列
     */
    public static final String EXCHANGE_DIRECT_ORDER_CANCEL = "exchange.direct.order.cancel";//"exchange.direct.order.create" test_exchange;
    public static final String ROUTING_ORDER_CANCEL = "order.create";
    //延迟取消订单队列
    public static final String QUEUE_ORDER_CANCEL  = "queue.order.cancel";
    //取消订单 延迟时间 单位：秒
    public static final int DELAY_TIME  = 2*60;

}