package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.list.dao.GoodsDao;
import com.atguigu.gmall.list.service.ListService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchAttr;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Administrator
 * @create 2020-05-21 16:00
 */
@Service
public class ListServiceImpl implements ListService {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private GoodsDao goodsDao;
    @Autowired
    private RedisTemplate redisTemplate;
//    @Autowired
//    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    private RestHighLevelClient restHighLevelClient;//ES原生客户端


    //上架商品  保存索引库
    @Override
    public void upperGoods(Long skuId) {
        Goods goods = new Goods();
        //1.skuInfo        根据映射类添加
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        goods.setId(skuInfo.getId());
        goods.setTitle(skuInfo.getSkuName());
        goods.setPrice(skuInfo.getPrice().doubleValue());
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        //2.品牌  id,名称，logo
        BaseTrademark baseTrademark = productFeignClient.getBaseTrademark(skuInfo.getTmId());
        goods.setTmId(baseTrademark.getId());
        goods.setTmName(baseTrademark.getTmName());
        goods.setTmLogoUrl(baseTrademark.getLogoUrl());
        //3.一二三级分类的id，名称
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        goods.setCategory1Id(categoryView.getCategory1Id());
        goods.setCategory2Id(categoryView.getCategory2Id());
        goods.setCategory3Id(categoryView.getCategory3Id());
        goods.setCategory1Name(categoryView.getCategory1Name());
        goods.setCategory2Name(categoryView.getCategory2Name());
        goods.setCategory3Name(categoryView.getCategory3Name());
        //4.平台属性集合
        List<SkuAttrValue> attrList = productFeignClient.getAttrList(skuId);
        //映射需要的时SearchAttr类型
        List<SearchAttr> searchAttrList = attrList.stream().map(skuAttrValue -> {
            SearchAttr searchAttr = new SearchAttr();
            searchAttr.setAttrId(skuAttrValue.getBaseAttrInfo().getId());
            searchAttr.setAttrName(skuAttrValue.getBaseAttrInfo().getAttrName());
            searchAttr.setAttrValue(skuAttrValue.getBaseAttrValue().getValueName());
            return searchAttr;
        }).collect(Collectors.toList());
        goods.setAttrs(searchAttrList);//SearchAttr
        //5.时间
        goods.setCreateTime(new Date());
        //6.保存索引
        goodsDao.save(goods);
    }

    //下架商品  删除索引库
    @Override
    public void lowerGoods(Long skuId) {
        goodsDao.deleteById(skuId);
    }

    //更新商品incrHotScore
    @Override
    public void incrHotScore(Long skuId) {
        //每次都更新索引库不好  使用每10分再更新索引库
        //使用redis缓存  先加到缓存里
        String hotScore = "hotScore";
        Double score = redisTemplate.opsForZSet().incrementScore(hotScore, skuId, 1);
        if(score%10 == 0){

            //从索引库查询
            Optional<Goods> optional = goodsDao.findById(skuId);
            Goods goods = optional.get();
            //追加热点分
            goods.setHotScore(Math.round(score));//四舍五入
            //更新时会先删除索引库 再添加
            goodsDao.save(goods);
        }
    }

    //搜索
    @Override
    public SearchResponseVo list(SearchParam searchParam) {
        //1.构建SearchRequest 条件对象
        SearchRequest searchRequest = buildSearchRequest(searchParam);
        try {
            //2.执行搜索
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //3.解析结果
            SearchResponseVo searchResponseVo = parseSearchResponse(searchResponse);
            return searchResponseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //3.解析结果 解析4部分
    private SearchResponseVo parseSearchResponse(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        SearchHits hits = searchResponse.getHits();


        //1.商品集合
        SearchHit[] searchHits = hits.getHits();
        List<Goods> goodsList = Arrays.stream(searchHits).map((h) -> {
            String sourceAsString = h.getSourceAsString();
            Goods goods = JSONObject.parseObject(sourceAsString, Goods.class);
            return goods;
        }).collect(Collectors.toList());
        //2.品牌集合

        //3.总条数
        long totalHits = hits.getTotalHits();
        searchResponseVo.setTotal(totalHits);
        searchResponseVo.setGoodsList(goodsList);

        return searchResponseVo;
    }

    //构建搜素条件对象
    private SearchRequest buildSearchRequest(SearchParam searchParam) {
        //构建资源条件对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //1.关键词 搜素字段 进行分词的  不能为null
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());//查询所有
        //2.品牌信息

        //3.一二三级分类

        //4.平台属性

        //5.排序

        //6.分页（当前页，总页数）

        //搜素请求对象
        SearchRequest searchRequest = new SearchRequest();
        //设置构建条件资源对象
        searchRequest.indices("goods");//设置索引库的名称
        searchRequest.types("info");//设置类型 可有可无
        searchRequest.source(searchSourceBuilder);

        return searchRequest;
    }
}
