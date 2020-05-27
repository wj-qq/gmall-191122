package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 全局过滤器   同一鉴权  路由
 * @author Administrator
 * @create 2020-05-25 21:33
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    //按照路径匹配规则
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Autowired
    private RedisTemplate redisTemplate;
    @Value("${auth.url}")
    private String[] authUrl;
    @Value("${location.baseUrl}")
    private String baseUrl;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取当前的uri 和url
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String path = request.getURI().getPath();//uri

        //不允许访问内部资源（路径包含/inner）  直接返回无访问权
        if(antPathMatcher.match("/inner/**",path)){//内部资源。不许访问
            return out(response, ResultCodeEnum.PERMISSION);
        }

        String userId = getUserId(request);
        String userTempId = getUserTempId(request);

        //访问权限资源  有不登陆可访问  有必须登录后才能访问的（路径包含/auth）
        if(antPathMatcher.match("/auth/**",path) && StringUtils.isEmpty(userId)) {//必须登录才可访问
            return out(response,ResultCodeEnum.LOGIN_AUTH);//未登录
        }

        //3:判断是否为刷新页面 （重定向到登录页面去）
        for (String url : authUrl) {
            if(path.contains(url) && StringUtils.isEmpty(userId)){
                //重定向 登录页面
                response.setStatusCode(HttpStatus.SEE_OTHER);//303 重定向
                try {
                    String URL = request.getURI().getRawSchemeSpecificPart();
                    response.getHeaders().set(HttpHeaders.LOCATION, baseUrl + URLEncoder.encode(URL,"utf-8"));
                    return response.setComplete();//设置完成，响应浏览器

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }

        //获取用户id 向后传递
        if(!StringUtils.isEmpty(userId)){
            //创建一个新的请求，并设置用户id
            request.mutate().header("userId",userId);
        }

        if(!StringUtils.isEmpty(userTempId)){
            //创建一个新的请求，并设置临时用户id
            request.mutate().header("userTempId",userTempId);
        }


        //放行
        return chain.filter(exchange);
    }

    private Mono<Void> out(ServerHttpResponse response,ResultCodeEnum resultCodeEnum) {
        String result = JSONObject.toJSONString(Result.build(null, resultCodeEnum));
        DataBufferFactory dataBufferFactory = response.bufferFactory();
        DataBuffer wrap = dataBufferFactory.wrap(result.getBytes());
        //需要设置编码  响应体内容
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE,"application/json;charset=utf-8");

        return response.writeWith(Mono.just(wrap));//设置响应体
    }

    private String getUserTempId(ServerHttpRequest request) {
        //获取令牌  1.请求头中  2.cookie中
        String userTempId = request.getHeaders().getFirst("userTempId");//获取一个
        if (StringUtils.isEmpty(userTempId)) {
            //从cookie中获取
            HttpCookie token1 = request.getCookies().getFirst("userTempId");
            if (token1 != null) {
                userTempId = token1.getValue();
            }
        }
        return userTempId;
    }

    private String getUserId(ServerHttpRequest request) {
        //获取令牌  1.请求头中  2.cookie中
        String token = request.getHeaders().getFirst("token");//获取一个
        if(StringUtils.isEmpty(token)){
            //从cookie中获取
            HttpCookie token1 = request.getCookies().getFirst("token");
            if(token1 != null){
                token = token1.getValue();
            }
        }

        if(!StringUtils.isEmpty(token)){
            //利用token拿到用户id
            if(redisTemplate.hasKey("user:login:" + token)){
                return (String) redisTemplate.opsForValue().get("user:login:" + token);
            }
        }
        return null;
    }


    //过滤器优先级从最大负整到最大正整
    @Override
    public int getOrder() {
        return 0;
    }
}
