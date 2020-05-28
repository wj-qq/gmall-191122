package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        CartInfo cartInfo = (CartInfo) redisTemplate.opsForHash().get(cartKey,skuId.toString());
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
        loadCartCache(cartKey);
        return cartInfo;
    }

    //去购物车结算
    @Override
    public List<CartInfo> cartList(String userId, String userTempId) {
        if(StringUtils.isEmpty(userId)){
            //未登录 查询临时用户的购物车
            return this.cartList(userTempId);
        }else{
            if(StringUtils.isEmpty(userTempId)){//已登录 查询登录用户的购物车
                return this.cartList(userId);
            }else {
                //需要合并购物车  临时==>登录
                return this.mergeCartList(userId,userTempId);
            }
        }
    }

    //购物车的 选中或取消
    @Override
    public void checkCart(Long skuId, Integer isChecked, String userId) {
        //跟新数据库 选中状态
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        cartInfoMapper.update(cartInfo,new QueryWrapper<CartInfo>().eq("user_id", userId)
                .eq("sku_id",skuId));

        //跟新redis选中状态
        String cartKey = cacheCartKey(userId);
        CartInfo cartInfo1 = (CartInfo) redisTemplate.opsForHash().get(cartKey, skuId.toString());
        cartInfo1.setIsChecked(isChecked);
        redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfo1);

    }

    //删除购物车
    @Override
    public void deleteCart(Long skuId, String userId) {
        //删除DB
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("sku_id",skuId));
        //删除缓存
        String cartKey = cacheCartKey(userId);
        redisTemplate.opsForHash().delete(cartKey,skuId.toString());

    }

    //结算 获取购买商品集合
    @Override
    public List<CartInfo> getCartCheckedList(Long userId) {
        //先查缓存
        String cartKey = cacheCartKey(userId.toString());
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if(CollectionUtils.isEmpty(cartInfoList)){
            //没有查DB
            cartInfoList = cartInfoMapper.selectList(new QueryWrapper<CartInfo>().eq("user_id",userId)
                    .eq("is_checked",1));
        }else {
            //过滤出缓存中选中的商品
            cartInfoList = cartInfoList.stream().filter(cartInfo -> cartInfo.getIsChecked().intValue() == 1 ? true : false)
                    .collect(Collectors.toList());
        }

        //需查询实时价格
        cartInfoList.forEach(cartInfo -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(cartInfo.getSkuId());
            cartInfo.setSkuPrice(skuPrice);
        });

        return cartInfoList;
    }

    //需要合并购物车  临时==>登录
    private List<CartInfo> mergeCartList(String userId, String userTempId) {
        //获取临时用户和登录用户的购物车商品集合
        List<CartInfo> cartInfoList = this.cartList(userId);
        List<CartInfo> cartInfoListTemp = this.cartList(userTempId);

        if(CollectionUtils.isEmpty(cartInfoListTemp)){
            return cartInfoList;
        }

        //将登录用户的购物车list转换为map，方便检索skuId
        Map<Long, CartInfo> cartInfoMap = cartInfoList.stream().collect(Collectors.toMap(
                cartInfo -> cartInfo.getSkuId(), cartInfo -> cartInfo));

        //将临时的购物车集合  集中到 登录用户上 对临时用户购物车集合进行遍历
        for (CartInfo cartInfoTemp : cartInfoListTemp) {
            CartInfo cartInfo = cartInfoMap.get(cartInfoTemp.getSkuId());
            if(cartInfo != null){
                //说明登录购物车中以存在 追加数量即可
                cartInfo.setSkuNum(cartInfo.getSkuNum() + cartInfoTemp.getSkuNum());
                //判断临时用户的购物车中选中是否为1
                if(cartInfoTemp.getIsChecked().intValue() == 1){
                    cartInfo.setIsChecked(1);
                }
                //跟新数据库 并删除原有临时用户的此商品
                cartInfoMapper.updateById(cartInfo);
                cartInfoMapper.deleteById(cartInfoTemp.getId());
            }else {
                //添加购物车
                cartInfoMap.put(cartInfoTemp.getSkuId(),cartInfoTemp);
                //表中把临时用户的用户id更改为登录用户即可  不用再删除了
                cartInfoTemp.setUserId(userId);
                cartInfoMapper.updateById(cartInfoTemp);
            }
        }
        //合并完后统一将原有缓存删除
        String cartKey = cacheCartKey(userId);
        String cartKeyTemp = cacheCartKey(userTempId);
        if(redisTemplate.hasKey(cartKeyTemp)){
            redisTemplate.delete(cartKeyTemp);//直接删除整个map， 即全部缓存
            redisTemplate.delete(cartKey);
        }

//      return (List<CartInfo>) cartInfoMap.values();不行 强制向下转型 会报错
        return new ArrayList<>(cartInfoMap.values());
    }

    //根据用户ID （可能是真实用户 可能是临时用户） 查询购物车集合
    private List<CartInfo> cartList(String userId) {
        //优先查询缓存、
        String cacheCartKey = cacheCartKey(userId);
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cacheCartKey);
        if(CollectionUtils.isEmpty(cartInfoList)){
            //从数据库查询 并放入缓存
            cartInfoList = loadCartCache(userId);
        }
        //实时价格
        cartInfoList.forEach(cartInfo -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(cartInfo.getSkuId());
            cartInfo.setSkuPrice(skuPrice);
        });
        //商品加入购物车的排序
        cartInfoList.sort((o1, o2) -> o2.getId().toString().compareTo(o1.getId().toString()));

        return cartInfoList;
    }

    //查询DB后放入缓存
    private List<CartInfo> loadCartCache(String userId) {
        String cacheCartKey = cacheCartKey(userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(new QueryWrapper<CartInfo>().eq("user_id", userId));
        if(!CollectionUtils.isEmpty(cartInfoList)){
            //转换为map,便于直接放到缓存
            Map<String, CartInfo> cartInfoMap = cartInfoList.stream().collect(Collectors.toMap(
                    cartInfo -> cartInfo.getSkuId().toString(), cartInfo -> cartInfo));

            //添加到缓存
            redisTemplate.opsForHash().putAll(cacheCartKey,cartInfoMap);
            setCartKeyExpire(cacheCartKey);
            return cartInfoList;
        }else {
           return new ArrayList<>();
        }
    }

    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey,RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    private String cacheCartKey(String userId) {
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }
}
