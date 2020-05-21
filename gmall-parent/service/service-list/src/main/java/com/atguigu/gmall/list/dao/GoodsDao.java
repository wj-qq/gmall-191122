package com.atguigu.gmall.list.dao;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author Administrator
 * @create 2020-05-21 15:52
 */
public interface GoodsDao extends ElasticsearchRepository<Goods,Long> {
}
