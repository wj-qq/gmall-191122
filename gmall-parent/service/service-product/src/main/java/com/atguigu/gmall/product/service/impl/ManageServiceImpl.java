package com.atguigu.gmall.product.service.impl;

import com.alibaba.nacos.common.util.UuidUtils;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.mq.MqConst;
import com.atguigu.gmall.mq.RabbitService;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 * @author Administrator
 * @create 2020-05-12 22:56
 */
@Service
public class ManageServiceImpl implements ManageService {


    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;
    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RabbitService rabbitService;

    //获取商品一级分类
    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    //获取商品二级分类
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        return baseCategory2Mapper.selectList(new QueryWrapper<BaseCategory2>().eq("category1_id", category1Id));
    }

    //获取商品三级分类
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        return baseCategory3Mapper.selectList(new QueryWrapper<BaseCategory3>().eq("category2_id", category2Id));
    }

    //根据分类id获取平台属性
    @Override
    public List<BaseAttrInfo> attrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        return baseAttrInfoMapper.attrInfoList(category1Id,category2Id,category3Id);
    }

    //添加平台属性//修改平台属性
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        if(baseAttrInfo.getId() != null){
            //修改数据
            baseAttrInfoMapper.updateById(baseAttrInfo);
        }else{
            //保存基本属性信息
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        // baseAttrValue 平台属性值
        // 修改：通过先删除{baseAttrValue}，在新增的方式！
        // 删除条件：baseAttrValue.attrId = baseAttrInfo.id
        baseAttrValueMapper.delete(new QueryWrapper<BaseAttrValue>().eq("attr_id", baseAttrInfo.getId()));
        //保存基本属性值信息
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        attrValueList.forEach(attrValue -> {
            attrValue.setAttrId(baseAttrInfo.getId());
            baseAttrValueMapper.insert(attrValue);
        });
    }

    //根据平台属性ID获取平台属性
    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        return baseAttrValueMapper.selectList(new QueryWrapper<BaseAttrValue>().eq("attr_id", attrId));
    }

    //获取销售属性
    @Override
    public List<BaseSaleAttr> baseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }

    //获取spu分页列表
    @Override
    public IPage<SpuInfo> spuPageList(Integer page, Integer limit, Long category3Id) {
        return spuInfoMapper.selectPage(new Page<SpuInfo>(page, limit),
                new QueryWrapper<SpuInfo>().eq("category3_id", category3Id));
    }

    //获取品牌属性
    @Override
    public List<BaseTrademark> getTrademarkList() {
        return baseTrademarkMapper.selectList(null);
    }

    //添加spu
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        //保存商品基本信息spu_info表
        spuInfoMapper.insert(spuInfo);
        //保存图片表
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if(!CollectionUtils.isEmpty(spuImageList)) {
            spuImageList.forEach(spuImage -> {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            });
        }
        //保存商品的销售属性和销售属性值表
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        spuSaleAttrList.forEach(spuSaleAttr -> {
            spuSaleAttr.setSpuId(spuInfo.getId());
            spuSaleAttrMapper.insert(spuSaleAttr);
            //属性值
            List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
            spuSaleAttrValueList.forEach(spuSaleAttrValue -> {
                spuSaleAttrValue.setSpuId(spuInfo.getId());
                spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                spuSaleAttrValueMapper.insert(spuSaleAttrValue);
            });
        });
    }

    //根据spuId获取图片列表
    @Override
    public List<SpuImage> spuImageList(Long spuId) {
        return spuImageMapper.selectList(new QueryWrapper<SpuImage>().eq("spu_id", spuId));
    }

    //根据spuId获取销售属性
    @Override
    public List<SpuSaleAttr> spuSaleAttrList(Long spuId) {
        return spuSaleAttrMapper.spuSaleAttrList(spuId);
    }

    //添加sku
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //保存sku基本信息表
        skuInfo.setIsSale(0);
        skuInfoMapper.insert(skuInfo);
        //保存sku图片表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        skuImageList.forEach(skuImage -> {
            skuImage.setSkuId(skuInfo.getId());
            skuImageMapper.insert(skuImage);
        });
        //保存sku销售属性表
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if(!CollectionUtils.isEmpty(skuAttrValueList)){
            skuAttrValueList.forEach(skuAttrValue -> {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            });
        }
        //保存销售属性值表
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if(!CollectionUtils.isEmpty(skuSaleAttrValueList)){
            skuSaleAttrValueList.forEach(skuSaleAttrValue -> {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            });
        }
    }

    //获取sku分页列表
    @Override
    public IPage<SkuInfo> skuPageList(Integer page, Integer limit) {
        return skuInfoMapper.selectPage(new Page<SkuInfo>(page,limit),null);
    }

    //上架商品
    @Override
    public void onSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);
        //发消息 给 搜索微服务
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);
    }

    //下架商品
    @Override
    public void cancelSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(0);
        skuInfo.setId(skuId);
        skuInfoMapper.updateById(skuInfo);
        //发消息 给 搜索微服务
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);

    }

    //使用redisson缓存及分布式锁
    public SkuInfo getSkuInfoRedisson(Long skuId) {
        String cacheKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
        String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;

        SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(cacheKey);
        if (skuInfo != null){//返回缓存中数据
            return skuInfo;
        }else {//缓存没有，才查询数据库
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean isLock = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1,RedisConst.SKULOCK_EXPIRE_PX2,TimeUnit.SECONDS);//1.尝试等待锁的时间 2.锁的过期时间
                if(isLock){//拿到锁 //查询数据库
                    skuInfo = skuInfoMapper.selectById(skuId);
                    if(skuInfo != null){
                        //查询图片信息
                        List<SkuImage> skuImages = skuImageMapper.selectList(new QueryWrapper<SkuImage>().eq("sku_id", skuId));
                        skuInfo.setSkuImageList(skuImages);
                        //缓存雪崩（大量缓存同时过期，请求过大） 在过期时间上加上随机数
                        int i = new Random().nextInt(5);
                        redisTemplate.opsForValue().set(cacheKey,skuInfo,RedisConst.SKUKEY_TIMEOUT + i,TimeUnit.SECONDS);
                    }else{//缓存穿透  可返回空结果
                        skuInfo = new SkuInfo();
                        redisTemplate.opsForValue().set(cacheKey,skuInfo,5, TimeUnit.MINUTES);
                    }

                }else{//没拿到锁
                    //防止缓存击穿，其他请求先睡一会再去查询
                   Thread.sleep(1000);
                    this.getSkuInfoRedisson(skuId);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {//释放锁
                if(lock.isLocked()) {
                    lock.unlock();
                }
            }
        }
        return skuInfo;
    }

    //根据skuId获取sku信息
    //使用redis分布式锁
    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        String cacheKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
        String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
        SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(cacheKey);
        if (skuInfo != null){//返回缓存中数据
            return skuInfo;
        }else{//缓存没有，才查询数据库
            //相当于setnx falg = 1 第一次查询 已上锁  flag=0 不是第一次， 未上锁
            //uuid防误删 防止代码没执行完时，锁已自动释放，防止误删锁
            String uuid = UuidUtils.generateUuid();
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, 2, TimeUnit.SECONDS);
            if(flag){//缓存击穿（大量请求同时访问同一数据库） 加分布式锁
                skuInfo = skuInfoMapper.selectById(skuId);
                if(skuInfo != null){
                    //查询图片信息
                    List<SkuImage> skuImages = skuImageMapper.selectList(new QueryWrapper<SkuImage>().eq("sku_id", skuId));
                    skuInfo.setSkuImageList(skuImages);
                    //缓存雪崩（大量缓存同时过期，请求过大） 在过期时间上加上随机数
                    int i = new Random().nextInt(5);
                    redisTemplate.opsForValue().set(cacheKey,skuInfo,RedisConst.SKUKEY_TIMEOUT + i,TimeUnit.SECONDS);
                }else{//缓存穿透  可返回空结果
                    skuInfo = new SkuInfo();
                    redisTemplate.opsForValue().set(cacheKey,skuInfo,5, TimeUnit.MINUTES);
                }
                //可以防误删，但是代码执行步骤多，缺乏原子性
                //使用LUA脚本  有原子性
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return tostring(redis.call('del',KEYS[1])) else return 0 end";
                this.redisTemplate.execute(new DefaultRedisScript<>(script), Collections.singletonList(lockKey), uuid);

//                if(uuid.equals(redisTemplate.opsForValue().get(lockKey))){
//                    redisTemplate.delete(lockKey);//手动解锁
//                }
            }else{//没拿到锁
                //防止缓存击穿，其他请求先睡一会再去查询
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.getSkuInfo(skuId);
            }

        }
        return skuInfo;
    }

    //通过三级分类id查询分类信息
    @GmallCache(prefix = "getCategoryView")//通过AOP来实现redis缓存和分布式锁
    @Override
    public BaseCategoryView getCategoryView(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
        /*以下代码有问题
        String cacheKey = RedisConst.SKUKEY_PREFIX + category3Id + RedisConst.SKUKEY_SUFFIX;
        String lockKey = RedisConst.SKUKEY_PREFIX + category3Id + RedisConst.SKULOCK_SUFFIX;
        BaseCategoryView baseCategoryView = (BaseCategoryView) redisTemplate.opsForValue().get(cacheKey);
        if(baseCategoryView != null){
            return baseCategoryView;
        }else {
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean tryLock = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if(tryLock){//缓存击穿
                    baseCategoryView = baseCategoryViewMapper.selectById(category3Id);//查数据库
                    if(baseCategoryView != null){//缓存雪崩
                        redisTemplate.opsForValue().set(cacheKey,baseCategoryView,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                    }else {//缓存穿透
                        baseCategoryView = new BaseCategoryView();
                        redisTemplate.opsForValue().set(cacheKey,baseCategoryView,5, TimeUnit.MINUTES);
                    }
                }else{//没拿到锁
                    //重新查询
                    Thread.sleep(1000);
                    return (BaseCategoryView) redisTemplate.opsForValue().get(cacheKey);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                if(lock.isLocked()){
                    lock.unlock();
                }
            }
        }
        return baseCategoryView;*/
    }

    //获取sku价格
    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if(skuInfo != null){
            return skuInfo.getPrice();
        }
        return new BigDecimal("0");
    }

    //根据spuId，skuId 查询销售属性集合
    @GmallCache(prefix = "getSpuSaleAttrListCheckBySku")
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return spuSaleAttrMapper.getSpuSaleAttrListCheckBySku(skuId, spuId);
       /* String cacheKey = RedisConst.SKUKEY_PREFIX + skuId + spuId + RedisConst.SKUKEY_SUFFIX;
        String lockKey = RedisConst.SKUKEY_PREFIX + skuId + spuId + RedisConst.SKULOCK_SUFFIX;
        List<SpuSaleAttr> spuSaleAttrListCheckBySku = (List<SpuSaleAttr>) redisTemplate.opsForValue().get(cacheKey);
        if(!CollectionUtils.isEmpty(spuSaleAttrListCheckBySku)){
            return spuSaleAttrListCheckBySku;
        }else {
            RLock lock = redissonClient.getLock(lockKey);
            try {//缓存击穿
                boolean tryLock = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if(tryLock) {
                    spuSaleAttrListCheckBySku = spuSaleAttrMapper.getSpuSaleAttrListCheckBySku(skuId, spuId);
                    if (!CollectionUtils.isEmpty(spuSaleAttrListCheckBySku)) {//缓存雪崩
                        redisTemplate.opsForValue().set(cacheKey, spuSaleAttrListCheckBySku, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    } else {//缓存穿透
                        spuSaleAttrListCheckBySku = new ArrayList<>();
                        redisTemplate.opsForValue().set(cacheKey, spuSaleAttrListCheckBySku, 5, TimeUnit.MINUTES);
                    }
                }else {
                    Thread.sleep(1000);
                    spuSaleAttrListCheckBySku = (List<SpuSaleAttr>) redisTemplate.opsForValue().get(cacheKey);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                if(lock.isLocked()){
                    lock.unlock();
                }
            }
        }
        return spuSaleAttrListCheckBySku;*/
    }

    //根据spuId 查询map 集合属性  销售属性值的组合
    @GmallCache(prefix = "getSkuValueIdsMap")
    @Override
    public Map getSkuValueIdsMap(Long spuId) {
        Map result = new HashMap();
        List<Map> skuValueIdsMap = skuSaleAttrValueMapper.getSkuValueIdsMap(spuId);
        for (Map map : skuValueIdsMap) {
            result.put(map.get("value_ids"), map.get("sku_id"));
        }
        return result;

        /*
        String cacheKey = RedisConst.SKUKEY_PREFIX + spuId + RedisConst.SKUKEY_SUFFIX;
        String lockKey = RedisConst.SKUKEY_PREFIX + spuId + RedisConst.SKULOCK_SUFFIX;
        Map result = (Map) redisTemplate.opsForValue().get(cacheKey);
        Map<Object, Object> finalResult = new HashMap<>();
        if(!CollectionUtils.isEmpty(result)){
            return result;
        }else {
            RLock lock = redissonClient.getLock(lockKey);
            try {//缓存击穿
                boolean tryLock = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if(tryLock) {
                    List<Map> mapList = skuSaleAttrValueMapper.getSkuValueIdsMap(spuId);
                    if(!CollectionUtils.isEmpty(mapList)){
                        mapList.forEach(map -> {
                            finalResult.put(map.get("value_ids"),map.get("sku_id"));
                        });
                    }
                    if (!CollectionUtils.isEmpty(finalResult)) {//缓存雪崩
                        redisTemplate.opsForValue().set(cacheKey, finalResult, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    } else {//缓存穿透
                        redisTemplate.opsForValue().set(cacheKey, finalResult, 5, TimeUnit.MINUTES);
                    }
                }else {
                    Thread.sleep(1000);
                    return (Map) redisTemplate.opsForValue().get(cacheKey);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                if(lock.isLocked()){
                    lock.unlock();
                }
            }
        }
        return finalResult;*/
    }

    //获取全部分类信息  首页使用
    @Override
    public List<Map> getBaseCategoryList() {
        //全部分类信息   需要对其封装为前端页面需要的格式
        List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(null);

        //存放最终结果
        List<Map> result = new ArrayList<>();

        //一级分类的分组数据集合  key为id，v
//        baseCategoryViews.stream().collect(Collectors.groupingBy(baseCategoryView -> {baseCategoryView.getCategory1Id()}));
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViews.stream().
                collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        int index = 1;
        for (Map.Entry<Long, List<BaseCategoryView>> category1Entry : category1Map.entrySet()) {
            Map map1 = new HashMap<>();
            Long category1Id = category1Entry.getKey();
            //一级分类对应的 二级分类数据集合
            List<BaseCategoryView> category2List = category1Entry.getValue();
            map1.put("index",index++);
            map1.put("categoryId",category1Id);
            map1.put("categoryName",category2List.get(0).getCategory1Name());
            //获取二级分类 的 分组数据集合
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().
                    collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //存放二级分类信息集合
            List<Map> category2 = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> category2Entry : category2Map.entrySet()) {
                Map map2 = new HashMap<>();
                List<BaseCategoryView> category3List = category2Entry.getValue();
                map2.put("categoryId",category2Entry.getKey());
                map2.put("categoryName",category3List.get(0).getCategory2Name());
                //存放三级分类信息集合
                List<Map> category3 = new ArrayList<>();
                for (BaseCategoryView baseCategoryView : category3List) {
                    Map map3 = new HashMap<>();
                    map3.put("categoryId",baseCategoryView.getCategory3Id());
                    map3.put("categoryName",baseCategoryView.getCategory3Name());
                    category3.add(map3);
                }
                map2.put("categoryChild",category3);
                category2.add(map2);
            }
            map1.put("categoryChild",category2);
            result.add(map1);
        }
        return result;
    }

    //根据tmId 查询品牌信息
    @Override
    public BaseTrademark getBaseTrademark(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }


    //根据skuId 查询平台属性值集合
    @Override
    public List<SkuAttrValue> getAttrList(Long skuId) {
        return skuAttrValueMapper.getAttrList(skuId);
    }
}
