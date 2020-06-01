package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Administrator
 * @create 2020-06-01 18:35
 */
@Component
public class DeadLetterMqConfig {

    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    //交换机
    @Bean
    public DirectExchange directExchange(){
        return ExchangeBuilder.directExchange(exchange_dead).build();
    }

    //ttl队列 基于死信的延迟消息队列
    @Bean
    public Queue queue1(){
        Map<String, Object> arguments = new HashMap<>();
        //1:过期转发的交换机
        arguments.put("x-dead-letter-exchange", exchange_dead);
        //2:过期转发的交换机的RoutingKey
        arguments.put("x-dead-letter-routing-key", routing_dead_2);
        //3:过期时间  （全局过期时间）   优化级低于局部过期时间
        arguments.put("x-message-ttl", 10 * 1000);//10s
        return QueueBuilder.durable(queue_dead_1).withArguments(arguments).build();
    }

    //正常队列
    @Bean
    public Queue queue2(){
        return QueueBuilder.durable(queue_dead_2).build();
    }

    //绑定1
    @Bean
    public Binding bindingQueue1(){
        return BindingBuilder.bind(queue1()).to(directExchange()).with(routing_dead_1);
    }

    //绑定2
    @Bean
    public Binding bindingQueue2(){
        return BindingBuilder.bind(queue2()).to(directExchange()).with(routing_dead_2);
    }

}
