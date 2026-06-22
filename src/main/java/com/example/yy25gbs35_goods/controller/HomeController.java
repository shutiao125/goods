package com.example.yy25gbs35_goods.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yy25gbs35_goods.goods.GoodsResultMapVO;
import com.example.yy25gbs35_goods.goods.User;
import com.example.yy25gbs35_goods.service.GoodsService;
import com.example.yy25gbs35_goods.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {
    
    @Autowired
    private GoodsService goodsService;
    
    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String listGoods(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate,
                           @RequestParam(defaultValue = "1") Integer pageNum,
                           Model model){
        // 获取当前登录用户信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            // 未登录用户，重定向到登录页面
            return "redirect:/login";
        }
        
        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);
        if (currentUser == null) {
            // 用户不存在，重定向到登录页面
            return "redirect:/login";
        }
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("userRole", currentUser.getRole());

        // 判断是否为管理员或卖家（可以编辑和删除商品的角色）
        boolean canEdit = "admin".equals(currentUser.getRole()) || "seller".equals(currentUser.getRole());
        model.addAttribute("canEditGoods", canEdit);

        // 所有登录用户都可以查看商品列表
        // 普通用户只能浏览，管理员/卖家可以编辑和删除

        Integer pageSize = 12;
        try {
            Page<GoodsResultMapVO> page;
            
            // 如果是卖家，只显示该卖家发布的商品
            if ("seller".equals(currentUser.getRole())) {
                page = goodsService.getSellerGoodsByPage(currentUser.getId().longValue(), pageNum, pageSize, keyword, startDate, endDate);
            } else {
                // 管理员显示所有商品（上架和缺货）
                page = goodsService.getGoodsResultMapVOsByPage(pageNum, pageSize, keyword, startDate, endDate);
            }
            
            model.addAttribute("goods", page.getRecords());
            model.addAttribute("page", page);
            model.addAttribute("keyword", keyword);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("pageNum", pageNum);
            model.addAttribute("pageSize", pageSize);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "加载商品列表失败：" + e.getMessage());
            model.addAttribute("goods", null);
        }
        return "goods-list";
    }
}