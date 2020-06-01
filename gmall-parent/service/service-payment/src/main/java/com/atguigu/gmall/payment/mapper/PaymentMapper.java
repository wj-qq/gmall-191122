package com.atguigu.gmall.payment.mapper;

import com.atguigu.gmall.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Administrator
 * @create 2020-06-01 22:00
 */
@Mapper
public interface PaymentMapper extends BaseMapper<PaymentInfo> {
}
