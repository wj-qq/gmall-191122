package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.atguigu.gmall.user.clent.UserFeignClient;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Administrator
 * @create 2020-05-28 16:12
 */
@RequestMapping("/api/order")
@RestController
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OrderInfoService orderInfoService;

    //对外暴露订单对象
    @GetMapping("/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable("orderId") Long orderId){
        return orderInfoService.getOrderInfo(orderId);
    }

  //订单页面  数据回显
    @GetMapping("/auth/trade")
    public Result<Map<String,Object>> trade(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);

        Map<String,Object> map = new HashMap<>();
        //地址集合
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(Long.parseLong(userId));
        //商品详情集合
        List<CartInfo> cartInfoList = cartFeignClient.getCartCheckedList(Long.parseLong(userId));
        List<OrderDetail> detailArrayList = cartInfoList.stream().map(cartInfo -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            return orderDetail;
        }).collect(Collectors.toList());

        //交易号  唯一标识，防止订单二次提交  返给缓存
        String tradeNoKey = "user:" + userId + ":tradeCode";
        String tradeNo = UUID.randomUUID().toString().replaceAll("-","");
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo);

        //商品总数量
        long totalNum = detailArrayList.stream().collect(Collectors.summarizingInt(OrderDetail::getSkuNum)).getSum();

        //购物车订单总金额
//        double totalAmount = detailArrayList.stream().collect(Collectors.summarizingDouble(orderDetail ->
//                orderDetail.getOrderPrice().doubleValue() * orderDetail.getSkuNum())).getSum();
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        orderInfo.sumTotalAmount();

        map.put("userAddressList",userAddressList);
        map.put("tradeNo",tradeNo);
        map.put("detailArrayList",detailArrayList);
        map.put("totalNum",totalNum);
        map.put("totalAmount",orderInfo.getTotalAmount());
        return Result.ok(map);
    }

    //订单提交订单
    @PostMapping("/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, String tradeNo, HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //从缓存中验证交易号
        String tradeNoKey = "user:" + userId + ":tradeCode";
        if(StringUtils.isEmpty(tradeNo)){
            return Result.fail().message("非法操作");
        }else{
            String tradeNoTemp = (String) redisTemplate.opsForValue().get(tradeNoKey);
            if(StringUtils.isEmpty(tradeNoTemp)){
                return Result.fail().message("请不要重复提交订单");
            }else {
                if(!tradeNoTemp.equals(tradeNo)){
                    return Result.fail().message("非法操作");
                }
            }
        }
        //提交订单  删除交易号（一次性使用的）
        redisTemplate.delete(tradeNoKey);
        //校验库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            boolean hasStock = orderInfoService.hasStock(orderDetail.getSkuId(),orderDetail.getSkuNum());
            if(!hasStock){
                return Result.fail().message(orderDetail.getSkuName() + ":库存不足");
            }
        }
        //保存订单表和订单详情表 //已购买的商品从购物车删除
        orderInfo.setUserId(Long.parseLong(userId));
        Long orderId = orderInfoService.submitOrder(orderInfo);

        return Result.ok(orderId);
    }


    //由库存微服务发出来的请求 要求折单
    @PostMapping("/orderSplit")
    public List<Map<String,Object>> orderSplit(String orderId, String wareSkuMap){
        //1:订单折分  入参：
        List<OrderInfo> orderInfoList = orderInfoService.orderSplit(orderId,wareSkuMap);//1个原始订单 至少折单 2个
        //2:返回值
        List<Map<String, Object>> mapList = orderInfoList.stream().map(orderInfo -> {
            return orderInfoService.initWareOrder(orderInfo);
        }).collect(Collectors.toList());
        return mapList;

    }
}
