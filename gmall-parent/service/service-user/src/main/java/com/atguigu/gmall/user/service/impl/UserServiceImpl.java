package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-05-25 20:45
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserAddressMapper userAddressMapper;


    //验证登录
    @Override
    public UserInfo login(UserInfo userInfo) {
        //加密输入的密码（数据库均为已加密密码）
        String digest = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        //根据用户名和密码e去数据库查询
        return userInfoMapper.selectOne(new QueryWrapper<UserInfo>().eq("login_name",userInfo.getLoginName())
                                        .eq("passwd",digest));
    }

    //订单页面  查询/显示 用户地址信息
    @Override
    public List<UserAddress> findUserAddressListByUserId(Long userId) {
        return userAddressMapper.selectList(new QueryWrapper<UserAddress>().eq("user_id",userId));
    }
}
