package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.bytebuddy.asm.Advice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @create 2020-05-26 0:51
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Autowired
    private ProductFeignClient productFeignClient;

    //加入购物车
    @Override
    public CartInfo addToCart(Long skuId, Integer skuNum, String userId) {
        //查询DB或缓存中是否已有该数据  如有 仅追加数量
        String cartKey = cacheCartKey(userId);
        CartInfo cartInfo = (CartInfo) redisTemplate.opsForValue().get(cartKey);
        if(cartInfo == null){//说明缓存没有  去数据库查询
            cartInfo = cartInfoMapper.selectOne(new QueryWrapper<CartInfo>().eq("sku_id", skuId)
                    .eq("user_id", userId));
        }
        //保存数据库数据
        if(cartInfo != null){//说明不是第一次添加购物车 更新数量和 价格即可
            cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);//更新数量
            //实时更新价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            cartInfo.setSkuPrice(skuPrice);
            //默认选中
            cartInfo.setIsChecked(1);
            cartInfoMapper.updateById(cartInfo);
        }else{//说明第一次添加购物车
            cartInfo = new CartInfo();
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuId(skuId);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setUserId(userId);
            cartInfo.setIsChecked(1);
            cartInfoMapper.insert(cartInfo);
        }

        //保存redis缓存
        redisTemplate.opsForValue().set(cartKey,cartInfo);
        setCartKeyExpire(cartKey);
        return cartInfo;
    }

    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey,RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    private String cacheCartKey(String userId) {
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }
}
