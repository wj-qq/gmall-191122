package com.atguigu.gmall.user.clent;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.clent.impl.UserDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-05-28 16:05
 */
@FeignClient(value = "service-user",fallback = UserDegradeFeignClient.class)
public interface UserFeignClient {

    //订单页面  查询/显示 用户地址信息
    @GetMapping("/api/user/passport/inner/findUserAddressListByUserId/{userId}")
    public List<UserAddress> findUserAddressListByUserId(@PathVariable("userId")Long userId);

}
