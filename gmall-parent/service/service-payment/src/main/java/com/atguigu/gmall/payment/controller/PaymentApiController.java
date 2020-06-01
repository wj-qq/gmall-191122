package com.atguigu.gmall.payment.controller;

import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author Administrator
 * @create 2020-06-01 21:46
 */
@RequestMapping("/api/payment/alipay")
@Controller
public class PaymentApiController {

    @Autowired
    private AlipayService alipayService;
    @Autowired
    private PaymentService paymentService;


    //支付宝支付
    @ResponseBody
    @GetMapping("/submit/{orderId}")
    public String submit(@PathVariable("orderId")Long orderId){
        return alipayService.submit(orderId);
    }

    //跳转至成功支付页面  商户
    @GetMapping("/callback/return")
    public String callbackReturn(){
        return "redirect:" + AlipayConfig.return_order_url;
    }

    //异步通知  支付宝支付成功之后异步回调  key value
    @PostMapping("/callback/notify")
    @ResponseBody
    public String callbackNotify(@RequestParam Map<String, String> paramsMap) throws Exception {
        //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap,
                AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);//调用SDK验证签名
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，
            //  对支付结果中的业务内容进行二次校验，
            //  校验成功后在response中返回success并继续商户自身业务处理，
            //  校验失败返回failure
            System.out.println(paramsMap);

            //{gmt_create=2020-06-01 14:50:31,
            // charset=utf-8,
            // gmt_payment=2020-06-01 14:50:49,
            // notify_time=2020-06-01 14:50:50,
            // subject=Apple iPhone 11 (A2223) 128GB 黑色 移动联通电信4G手机 双卡双待 Redmi K30 5G双模 120Hz流速屏 骁龙765G 30W快充 6GB+128GB 深海微光 游戏智能手机 小米 红米 ,
            // buyer_id=2088102181130586,
            // invoice_amount=31791.00,
            // version=1.0,
            // notify_id=2020060100222145050030580507689893,
            // fund_bill_list=[{"amount":"31791.00","fundChannel":"ALIPAYACCOUNT"}],
            // notify_type=trade_status_sync,
            // out_trade_no=ATGUIGU1590994219197258,
            // total_amount=31791.00,
            // trade_status=TRADE_SUCCESS,
            // trade_no=2020060122001430580501081972,
            // auth_app_id=2016102100732915,
            // receipt_amount=31791.00,
            // point_amount=0.00,
            // app_id=2016102100732915,
            // buyer_pay_amount=31791.00,
            // seller_id=2088102180533564}

            if ("TRADE_SUCCESS".equals(paramsMap.get("trade_status"))) {
                //更新支付信息表   四个字段
                paymentService.paySuccess(paramsMap);
            }else{
                return "failure";
            }
            return "success";
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
    }

    //退钱
    @GetMapping("/refund/{outTradeNo}")
    @ResponseBody
    public Result refund(@PathVariable(name = "outTradeNo") String outTradeNo){

        alipayService.refund(outTradeNo);
        return Result.ok();
    }


}
