package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.list.dao.GoodsDao;
import com.atguigu.gmall.list.service.ListService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
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
//            private Long total;//总记录数
//            private Integer pageSize;//每页显示的内容
//            private Integer pageNo;//当前页面
            searchResponseVo.setPageNo(searchParam.getPageNo());//1
            searchResponseVo.setPageSize(searchParam.getPageSize());//5
            searchResponseVo.setTotalPages((searchResponseVo.getTotal()+searchResponseVo.getPageSize()-1)
                    /searchResponseVo.getPageSize());
            return searchResponseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //3.解析结果 解析4部分
    private SearchResponseVo parseSearchResponse(SearchResponse searchResponse) {
        SearchResponseVo vo = new SearchResponseVo();
        SearchHits hits = searchResponse.getHits();
        //1.商品集合
        SearchHit[] searchHits = hits.getHits();
        List<Goods> goodsList = Arrays.stream(searchHits).map((h) -> {
            String sourceAsString = h.getSourceAsString();
            Goods goods = JSONObject.parseObject(sourceAsString, Goods.class);
            HighlightField title = h.getHighlightFields().get("title");
            if(title != null){
                String t = title.fragments()[0].toString();
                goods.setTitle(t);//设置显示高亮
            }
            return goods;
        }).collect(Collectors.toList());
        vo.setGoodsList(goodsList);
        //2.品牌集合
//        Aggregation tmIdAgg = searchResponse.getAggregations().asMap().get("tmIdAgg");
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) searchResponse.getAggregations().asMap().get("tmIdAgg");
        List<SearchResponseTmVo> responseTmVoList = tmIdAgg.getBuckets().stream().map(t -> {
            SearchResponseTmVo responseTmVo = new SearchResponseTmVo();

            responseTmVo.setTmId(Long.parseLong(t.getKeyAsString()));

            ParsedStringTerms tmNameAgg = (ParsedStringTerms) t.getAggregations().get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            responseTmVo.setTmName(tmName);

            ParsedStringTerms tmLogoUrlAgg = ((Terms.Bucket) t).getAggregations().get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();

            responseTmVo.setTmLogoUrl(tmLogoUrl);
            return responseTmVo;
        }).collect(Collectors.toList());
        vo.setTrademarkList(responseTmVoList);
        //3.解析平台属性集合
        ParsedNested attrsAgg = (ParsedNested) searchResponse.getAggregations().asMap().get("attrsAgg");
        ParsedLongTerms attrIdAgg = attrsAgg.getAggregations().get("attrIdAgg");
        List<SearchResponseAttrVo> responseAttrVoList = attrIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseAttrVo responseAttrVo = new SearchResponseAttrVo();

            responseAttrVo.setAttrId(Long.parseLong(bucket.getKeyAsString()));

            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            responseAttrVo.setAttrName(attrName);

            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
            List<String> attrValueList = attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            responseAttrVo.setAttrValueList(attrValueList);

            return responseAttrVo;
        }).collect(Collectors.toList());
        vo.setAttrsList(responseAttrVoList);
        //4.总条数
        long totalHits = hits.getTotalHits();
        vo.setTotal(totalHits);

        return vo;
    }

    //构建搜素条件对象
    private SearchRequest buildSearchRequest(SearchParam searchParam) {
        //构建资源条件对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();//多条件查询

        //1.关键词 搜素字段 进行分词的  不能为null
        String keyword = searchParam.getKeyword();
        if(StringUtils.isNotEmpty(keyword)){
            boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword));
        }
        //2.品牌信息
        String trademark = searchParam.getTrademark();
        if(StringUtils.isNotEmpty(trademark)){//格式   1:苹果
            String[] split = trademark.split(":");
            boolQueryBuilder.filter(QueryBuilders.termQuery("tmId",split[0]));//filter为追加
        }
        //3.一二三级分类
        Long category1Id = searchParam.getCategory1Id();
        if(category1Id != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id",category1Id));
        }
        Long category2Id = searchParam.getCategory2Id();
        if(category2Id != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id",category2Id));
        }
        Long category3Id = searchParam.getCategory3Id();
        if(category3Id != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id",category3Id));
        }
        //4.平台属性  props=23:4G:运行内存
        String[] props = searchParam.getProps();
        if(props != null && props.length > 0){
            for (String prop : props) {
                String[] split = prop.split(":");
                //嵌套查询    构建子查询
                BoolQueryBuilder subBoolQueryBuilder = QueryBuilders.boolQuery();
                subBoolQueryBuilder.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                subBoolQueryBuilder.must(QueryBuilders.termQuery("attrs.attrValue",split[1]));
                boolQueryBuilder.filter(QueryBuilders.nestedQuery("attrs",subBoolQueryBuilder, ScoreMode.None));
            }
        }
        searchSourceBuilder.query(boolQueryBuilder);//查询所有
        //5.排序  1:hotScore  2:price
        String order = searchParam.getOrder();
        if(StringUtils.isNotEmpty(order)){
            String[] split = order.split(":");
            String fieldName = "";
            switch (split[0]){
                case "1": fieldName = "hotScore"; break;
                case "2": fieldName = "price"; break;
            }
            searchSourceBuilder.sort(fieldName,"asc".equals(split[1])? SortOrder.ASC : SortOrder.DESC);
        }else {//无需排序 走默认
            searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        }
        //6.分页（当前页，总页数）
        searchSourceBuilder.from((searchParam.getPageNo()-1)*searchParam.getPageSize());//开始行
        searchSourceBuilder.size(searchParam.getPageSize());//每页数
        //隐藏设置  高亮和 分组
        //高亮
        HighlightBuilder highlighter = new HighlightBuilder();
        highlighter.field("title").preTags("<font color='red'>").postTags("</font>");
        searchSourceBuilder.highlighter(highlighter);
        //分组
        //品牌分组设置
        searchSourceBuilder.aggregation(AggregationBuilders.terms("tmIdAgg").field("tmId")//对tmId域进行分组
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl")));
        //平台属性分组设置
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrsAgg","attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));
        //搜素请求对象
        SearchRequest searchRequest = new SearchRequest();
        //设置构建条件资源对象
        searchRequest.indices("goods");//设置索引库的名称
        searchRequest.types("info");//设置类型 可有可无
        searchRequest.source(searchSourceBuilder);

        return searchRequest;
    }
}
