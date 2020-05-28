package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @create 2020-05-25 20:42
 */
@RestController
@RequestMapping("/api/user/passport")
public class UserApiController {

    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;


    //登录按钮  异步
    @PostMapping("/login")
    public Result login(@RequestBody UserInfo userInfo) {
        //判断用户名和密码不能为空
        if(StringUtils.isEmpty(userInfo.getLoginName()) || StringUtils.isEmpty(userInfo.getPasswd())){
            return Result.fail().message("用户名或密码不能为空");
        }
        //去数据库查询该用户信息  前端需返回昵称
        UserInfo user = userService.login(userInfo);
        if(user == null){
            return Result.fail().message("此用户名或密码不正确");
        }else{
            //用户名和密码正确后，生成token 并保存
            String token = UUID.randomUUID().toString().replaceAll("-","");
            //缓存中保存令牌  单点登录 后续进业务查询缓存中是否有
            redisTemplate.opsForValue().set( RedisConst.USER_LOGIN_KEY_PREFIX + token, user.getId().toString(),
                    RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            //返回token和昵称
            Map map = new HashMap<>();
            map.put("token",token);
            map.put("nickName",user.getNickName());

            return Result.ok(map);
        }
    }

    //退出登录
    @GetMapping("logout")
    public Result logout(HttpServletRequest request){
        //删除缓存中的token
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX + request.getHeader("token"));
        return Result.ok();
    }


    //订单页面  查询/显示 用户地址信息
    @GetMapping("inner/findUserAddressListByUserId/{userId}")
    public List<UserAddress> findUserAddressListByUserId(@PathVariable("userId")Long userId){
        return userService.findUserAddressListByUserId(userId);
    }

}


