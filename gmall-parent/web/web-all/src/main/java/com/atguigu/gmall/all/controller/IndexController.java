package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @create 2020-05-19 18:11
 */
@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private TemplateEngine templateEngine;

//    @RequestMapping("/")
//    public String index(Model model){
//        List<Map> baseCategoryList = productFeignClient.getBaseCategoryList();
//        model.addAttribute("list",baseCategoryList);
//        return "index/index";
//    }

    //首页生成静态页面
    @RequestMapping("/createHtml")
    @ResponseBody
    public Result createHtml(){
        //重点：静态化程序
        //1) 模板 就是页面   index/index
        //2) 准备数据
        String templates = ClassUtils.getDefaultClassLoader().getResource("templates").getPath();

        List<Map> baseCategoryList = productFeignClient.getBaseCategoryList();
        Context context = new Context();
        context.setVariable("list",baseCategoryList);

        Writer writer = null;
        try {
            //创建静态化页面到templates目录下
            writer = new PrintWriter(templates + "/index.html","utf-8");

            //3)由静态化程序  将上面数据与模板进行渲染
            // Thymeleaf 就是静态化技术   页面标签  th:text th:each
            //Thymeleaf : 在后端 静态化技术  +  前端 页面标签
            //参数1 ： 模板路径及模板名称
            //参数2：数据
            //参数3：输出流 （将页面通过输出流输出到指定的位置）  读取的位置
            templateEngine.process("index/index",context,writer);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(writer != null){
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Result.ok();
    }

    //2:直接访问此页面  进入首页
    @RequestMapping("/")
    public String index(){
        return "index";
    }

}
