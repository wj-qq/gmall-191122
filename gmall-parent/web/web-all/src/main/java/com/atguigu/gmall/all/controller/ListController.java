package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author Administrator
 * @create 2020-05-22 22:10
 */
@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;

    //搜索页面 入参 必须是对象
    @GetMapping("/list.html")
    public String list(SearchParam searchParam, Model model){
        //入参对象 需要进行回显
        model.addAttribute("searchParam",searchParam);
        //查询结果
        SearchResponseVo searchResponseVo = listFeignClient.list(searchParam);
        //品牌集合
        model.addAttribute("trademarkList",searchResponseVo.getTrademarkList());
        //平台属性集合
        model.addAttribute("attrsList",searchResponseVo.getAttrsList());
        //商品集合
        model.addAttribute("goodsList",searchResponseVo.getGoodsList());
        //分页 当前页 总页数
        model.addAttribute("totalPages",searchResponseVo.getTotalPages());
        model.addAttribute("pageNo",searchResponseVo.getPageNo());

        String urlParam = makeUrlParam(searchParam);
        model.addAttribute("urlParam",urlParam);
        return "list/index";
    }

    private String makeUrlParam(SearchParam searchParam) {
        StringBuilder sb = new StringBuilder();
        String keyword = searchParam.getKeyword();
        if(!StringUtils.isEmpty(keyword)){
            sb.append("keyword=").append(keyword);
        }
        String trademark = searchParam.getTrademark();
        if(!StringUtils.isEmpty(trademark)){
            if(sb.length() > 0){
                sb.append("&trademark=").append(trademark);
            }else {
                sb.append("trademark=").append(trademark);
            }
        }
        Long category1Id = searchParam.getCategory1Id();
        if(!StringUtils.isEmpty(category1Id)){
            if(sb.length() > 0){
                sb.append("&category1Id=").append(category1Id);
            }else {
                sb.append("category1Id=").append(category1Id);
            }
        }

        Long category2Id = searchParam.getCategory2Id();
        if(!StringUtils.isEmpty(category2Id)){
            if(sb.length() > 0){
                sb.append("&category2Id=").append(category2Id);
            }else {
                sb.append("category2Id=").append(category2Id);
            }
        }

        Long category3Id = searchParam.getCategory3Id();
        if(!StringUtils.isEmpty(category3Id)){
            if(sb.length() > 0){
                sb.append("&category3Id=").append(category3Id);
            }else {
                sb.append("category3Id=").append(category3Id);
            }
        }

        String[] props = searchParam.getProps();
        if(null != props && props.length > 0){
            for (String prop : props) {
                if(sb.length() > 0){
                    sb.append("&props=").append(prop);
                }else {
                    sb.append("props=").append(prop);
                }
            }
        }
        return  "/list.html?" + sb.toString();
    }

}
