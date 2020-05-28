package com.atguigu.gmall.user.clent.impl;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.clent.UserFeignClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-05-28 16:06
 */
@Component
public class UserDegradeFeignClient implements UserFeignClient {
    @Override
    public List<UserAddress> findUserAddressListByUserId(Long userId) {
        return null;
    }
}
