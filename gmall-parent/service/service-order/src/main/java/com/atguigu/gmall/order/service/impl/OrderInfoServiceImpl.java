package com.atguigu.gmall.order.service.impl;

import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.mq.MqConst;
import com.atguigu.gmall.mq.RabbitService;
import com.atguigu.gmall.order.mapper.CartInfoMapper;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.bytebuddy.asm.Advice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * @author Administrator
 * @create 2020-05-28 20:01
 */
@Service
public class OrderInfoServiceImpl implements OrderInfoService {

    @Value("${ware.url}")
    private String wareUrl;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitService rabbitService;



    //校验库存
    @Override
    public boolean hasStock(Long skuId, Integer skuNum) {
        // http://www.gware.com/hasStock?skuId=10221&num=2
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    ////保存订单表和订单详情表  //已购买的商品从购物车删除
    @Override
    @Transactional
    public Long submitOrder(OrderInfo orderInfo) {

        //保存订单表
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());//订单状态
        orderInfo.setCreateTime(new Date());//订单创建时间
        //订单的第三方支付交易号
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis();
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            outTradeNo += random.nextInt(10);//再追加三位数的随机数
        }
        orderInfo.setOutTradeNo(outTradeNo);
        //交易体
        String tradeBody = "";
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            tradeBody += orderDetail.getSkuName() + "  ";

        }
        orderInfo.sumTotalAmount();
        orderInfo.setTradeBody(tradeBody);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());//进度状态
        orderInfoMapper.insert(orderInfo);

        String cartKey = cacheCartKey(orderInfo.getUserId().toString());
        //订单详情表
        orderDetailList.forEach(orderDetail -> {
            orderDetail.setOrderPrice(productFeignClient.getSkuPrice(orderDetail.getSkuId())); //实时价格
            orderDetail.setOrderId(orderInfo.getId());//外键
            orderDetailMapper.insert(orderDetail);
            //删除已经购买了商品的购物车
//            cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id",orderInfo.getUserId())
//                    .eq("sku_id",orderDetail.getSkuId()));//删除数据库
//
//            redisTemplate.opsForHash().delete(cartKey,orderDetail.getSkuId().toString());//删除缓存
        });

        //发延迟消息
        rabbitService.sendDelayedMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,
                MqConst.ROUTING_ORDER_CANCEL,orderInfo.getId(),MqConst.DELAY_TIME);

        return orderInfo.getId();
    }

    //取消订单
    @Override
    public void cancelOrder(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if(OrderStatus.UNPAID.name().equals(orderInfo.getOrderStatus())){
            orderInfo.setOrderStatus(OrderStatus.CLOSED.name());
            orderInfo.setProcessStatus(ProcessStatus.CLOSED.name());
        }
    }


    //du对外暴露订单对象
    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(new QueryWrapper<OrderDetail>().eq("order_id", orderId));
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }


    //缓存的Key值  入参：用户ID   表明此购物车是哪个用户的
    private String cacheCartKey(String userId) {
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }
}
