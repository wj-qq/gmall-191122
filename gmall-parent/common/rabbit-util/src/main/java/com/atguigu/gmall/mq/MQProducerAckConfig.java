package com.atguigu.gmall.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @Description 消息发送确认
 * <p>
 * ConfirmCallback  只确认消息是否正确到达 Exchange 中
 * ReturnCallback   消息没有正确到达队列时触发回调，如果正确到达队列不执行
 * <p>
 * 1. 如果消息没有到exchange,则confirm回调,ack=false
 * 2. 如果消息到达exchange,则confirm回调,ack=true
 * 3. exchange到queue成功,则不回调return
 * 4. exchange到queue失败,则回调return
 * 
 */
@Component
@Slf4j
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate redisTemplate;


    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);            //指定 ConfirmCallback
        rabbitTemplate.setReturnCallback(this);             //指定 ReturnCallback
    }


    //交换机应答方法  接收交换机应答的消息  成功应答 失败也应答
    //参数1：消息相关信息
    //参数2：应答成功 true  还是失败  false
    //参数3：失败的原因
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            log.info("消息发送成功：" + JSON.toJSONString(correlationData));
        } else {
            log.info("消息发送失败：" + cause + " 数据：" + JSON.toJSONString(correlationData));
            //重新发送  上一次发送失败的时候的交换机及RoutingKey及消息体
            GmallCorrelationData gmallCorrelationData = (GmallCorrelationData)correlationData;
            this.retrySendMessage(gmallCorrelationData);
        }
    }
    //重新发送消息
    public void retrySendMessage(GmallCorrelationData gmallCorrelationData){
        //不能无限重发  2-3次机会
        int retryCount = gmallCorrelationData.getRetryCount();//0 1
        if(retryCount < 2){
            //追加次数
            gmallCorrelationData.setRetryCount(++retryCount);
            log.info("重新发送：" + JSON.toJSONString(gmallCorrelationData));
            //重新发送
//            rabbitTemplate.convertAndSend("exchange11",
//                    gmallCorrelationData.getRoutingKey(),gmallCorrelationData.getMessage()
//            ,gmallCorrelationData);
            //重新发送消息之后 更新缓存 为了让队列失败时获取缓存中的消息重发次数得到更新
            //为了防止 队列接收消息失败 应答的时候 需要使用到下面缓存信息
            redisTemplate.opsForValue().set(gmallCorrelationData.getId(),
                    JSONObject.toJSONString(gmallCorrelationData),5, TimeUnit.MINUTES);
            rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(),
                    gmallCorrelationData.getRoutingKey(),gmallCorrelationData.getMessage()
                    ,gmallCorrelationData);
        }
    }


    //队列应答  只有失败了才应答  成功是不应答
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        // 反序列化对象输出
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);
        //重新发送  次数做不到追加
       //Message message   相当于Request对象
        //Request对象  1：请求头  2：请求体
        //Message message  1：消息头  2：消息体
        //       消息头中有主键   发送消息的时候  第四个参数对象
        String uuid = message.getMessageProperties()
                .getHeader("spring_returned_message_correlation");
        if(StringUtils.isEmpty(uuid)){
            log.error("获取不到UUId无法进行消息重新发送");
            return;
        }
        String gmallJson = (String) redisTemplate
                .opsForValue().get(uuid);

        if(StringUtils.isEmpty(gmallJson)){
            log.error("获取不到GmallCorrelationData无法完成消息的发送");
            return;
        }
        GmallCorrelationData gmallCorrelationData = JSONObject.
                parseObject(gmallJson, GmallCorrelationData.class);
        //判断是不是延迟
        if(gmallCorrelationData.isDelay()){
            log.error("本次队列失败应答是正常的、不必重新发送消息");
            return;
        }

        System.out.println("队列应答失败：返回来的UUID：" + uuid);
        //可以重新发送消息
        this.retrySendMessage(gmallCorrelationData);

    }

 }