package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.model.user.UserInfo;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-05-25 20:45
 */
public interface UserService {

    //登录验证
    UserInfo login(UserInfo userInfo);

    //订单页面  查询/显示 用户地址信息
    List<UserAddress> findUserAddressListByUserId(Long userId);
}
