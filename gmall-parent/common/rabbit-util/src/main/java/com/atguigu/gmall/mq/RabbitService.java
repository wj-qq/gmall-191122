package com.atguigu.gmall.mq;

import com.alibaba.fastjson.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 封装发消息 MQ
 * @author Administrator
 * @create 2020-06-01 16:02
 */
@SuppressWarnings("all")
@Component
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate redisTemplate;


    //发消息（普通）
    public void sendMessage(String exchange, String routingKey, Object msg){

        //封装交换机 应答返回对象
        GmallCorrelationData  correlationData = new GmallCorrelationData();

        //主键
        String id = UUID.randomUUID().toString().replaceAll("-", "");
        correlationData.setId(id);
        System.out.println("发送消息的时候的主键：" + id);
        correlationData.setExchange(exchange);
        correlationData.setRoutingKey(routingKey);
        correlationData.setMessage(msg);

        //将上面的GmallCorrelationData 保存在Redis缓存中 Key：UUID主键  V：GmallCorrelationData
        //为了防止 队列接收消息失败 应答的时候 需要使用到下面缓存信息
        redisTemplate.opsForValue().set(id, JSONObject.toJSONString(correlationData), 5, TimeUnit.MINUTES);
        //发消息
        rabbitTemplate.convertAndSend(exchange,routingKey,msg,correlationData);
    }

    //发送延迟消息
    public void sendDelayedMessage(String exchange, String routingKey, Object msg, int delayTime){
        //封装交换机 应答返回对象
        GmallCorrelationData  correlationData = new GmallCorrelationData();

        //主键
        String id = UUID.randomUUID().toString().replaceAll("-", "");
        correlationData.setId(id);
        System.out.println("发送消息的时候的主键：" + id);
        correlationData.setExchange(exchange);
        correlationData.setRoutingKey(routingKey);
        correlationData.setMessage(msg);
        correlationData.setDelay(true);
        correlationData.setDelayTime(delayTime);

        //将上面的GmallCorrelationData 保存在Redis缓存中 Key：UUID主键  V：GmallCorrelationData
        //为了防止 队列接收消息失败 应答的时候 需要使用到下面缓存信息
        redisTemplate.opsForValue().set(id, JSONObject.toJSONString(correlationData), 5, TimeUnit.MINUTES);
        //发消息
        rabbitTemplate.convertAndSend(exchange,routingKey,msg,message -> {
            message.getMessageProperties().setDelay(correlationData.getDelayTime());
            System.out.println("发送延迟消息的时间：" + new Date());
            return message;
        },correlationData);
    }


}
