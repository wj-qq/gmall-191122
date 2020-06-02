package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSONObject;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

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
            orderDetail.setOrderPrice(productFeignClient.getSkuPrice(orderDetail.getSkuId())); //实时价格
            orderDetail.setOrderId(orderInfo.getId());//外键

        }
        orderInfo.sumTotalAmount();
        orderInfo.setTradeBody(tradeBody);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());//进度状态
        orderInfoMapper.insert(orderInfo);

        String cartKey = cacheCartKey(orderInfo.getUserId().toString());
        //订单详情表
        orderDetailList.forEach(orderDetail -> {

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

    //更新订单状态 进度状态  未支付--》 已支付
    @Override
    public void updateOrderStatus(Long orderId, OrderStatus os, ProcessStatus ps) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setOrderStatus(os.name());
        orderInfo.setProcessStatus(ps.name());

        orderInfoMapper.updateById(orderInfo);
    }

    public void updateOrderStatus(Long orderId,ProcessStatus ps) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(ps.name());

        orderInfoMapper.updateById(orderInfo);
    }


    //发消息 到库存微服务 扣减库存
    @Override
    public void sendOrderStatus(Long orderId) {
        //更新订单进度状态为 已通知仓库
        this.updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);

        //初始化一份数据 发消息给库存微服务 那边的数据
        String result = initWareOrder(orderId);
        //发消息给库存微服务  (请查看库存微服务Api文档）
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK,MqConst.ROUTING_WARE_STOCK,result);
    }

    //折单
    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        //原始订单
        OrderInfo orderInfoOrigin = this.getOrderInfo(Long.parseLong(orderId));
        //开始拆单   wareSkuMap  [{"wareId":"1","skuIds":["14"]},{"wareId":"2","skuIds":["1"]}]
        System.out.println(wareSkuMap);
        List<Map> wareSkuMapList = JSONObject.parseArray(wareSkuMap, Map.class);
        //拆单后订单集合
        List<OrderInfo> orderInfoList = new ArrayList<>();

        //  仓库的ID      库存IDS
        //{"wareId":"1","skuIds":["14,15"]}
        for (Map wareMap : wareSkuMapList) {
            //订单表
            OrderInfo subOrderInfo = new OrderInfo();
            //9成以上相似  将原始订单信息复制一份给子订单
            BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
            //ID   防止ID冲突  //将来保存的时候 由Db数据库来生成
            subOrderInfo.setId(null);
            //给子订单设置父ID 外键
            subOrderInfo.setParentOrderId(orderInfoOrigin.getId());
            //仓库ID
            subOrderInfo.setWareId((String)wareMap.get("wareId"));

            //订单详情表  //从原始订单获取出来的订单详情目前长度是2  实际1
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();

            List<String> skuIdList = (List<String>) wareMap.get("skuIds");

            //根据条件进行订单详情集合过滤
            List<OrderDetail> orderDetails = orderDetailList.stream().filter(orderDetail -> {
                for (String skuId : skuIdList) {
                    if (skuId.equals(orderDetail.getSkuId().toString())) {
                        //存在  不过滤掉
                        return true;
                    }
                }
                //不存在  过滤掉了
                return false;//true;或是false
            }).collect(Collectors.toList());
            //将订单详情集合设置到订单对象中
            subOrderInfo.setOrderDetailList(orderDetails);

            //操作订单表  订单详情表
            saveOrderInfo(subOrderInfo);

            //追加到集合中
            orderInfoList.add(subOrderInfo);
        }
        //将原始订单状态更新为已折单
        this.updateOrderStatus(orderInfoOrigin.getId(),OrderStatus.SPLIT,ProcessStatus.SPLIT);

        return orderInfoList;
    }

    //操作订单表  订单详情表
    private void saveOrderInfo(OrderInfo subOrderInfo) {
        //1:保存订单
        orderInfoMapper.insert(subOrderInfo);
        //2:订单详情表
        List<OrderDetail> orderDetailList = subOrderInfo.getOrderDetailList();
        orderDetailList.forEach(orderDetail -> {
            //外键
            orderDetail.setOrderId(subOrderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        });

    }


    public String initWareOrder(Long orderId) {
        OrderInfo orderInfo = this.getOrderInfo(orderId);
        //封装方法
        Map<String,Object> result = this.initWareOrder(orderInfo);
        return JSONObject.toJSONString(result);
    }

    public   Map<String,Object> initWareOrder(OrderInfo orderInfo) {
        Map<String, Object> result = new HashMap<>();
        result.put("orderId",orderInfo.getId());
        result.put("consignee",orderInfo.getConsignee());
        result.put("consigneeTel",orderInfo.getConsigneeTel());
        result.put("orderComment",orderInfo.getOrderComment());
        result.put("orderBody",orderInfo.getTradeBody());
        result.put("deliveryAddress",orderInfo.getDeliveryAddress());
        result.put("paymentWay","2");
        //仓库编号
        result.put("wareId",orderInfo.getWareId());

        //订单详情
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if(CollectionUtils.isEmpty(orderDetailList)){
            List<Map> listMap = orderDetailList.stream().map(orderDetail -> {
                Map map = new HashMap();
                map.put("skuId", orderDetail.getSkuId());
                map.put("skuNum", orderDetail.getSkuNum());
                map.put("skuName", orderDetail.getSkuName());
                return map;
            }).collect(Collectors.toList());

            result.put("details",listMap);
        }

        return result;
    }


    //缓存的Key值  入参：用户ID   表明此购物车是哪个用户的
    private String cacheCartKey(String userId) {
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }
}
