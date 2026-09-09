package com.example.goods.controller;

import com.example.goods.goods.User;
import com.example.goods.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisterController {

    @Autowired
    private UserService userService;

    @GetMapping("/toRegisterPage")
    public String toRegisterPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam("registerType") String registerType,
                          @RequestParam("username") String username,
                          @RequestParam("password") String password,
                          @RequestParam(value = "email", required = false) String email,
                          // 卖家特有字段
                          @RequestParam(value = "companyName", required = false) String companyName,
                          @RequestParam(value = "contactPhone", required = false) String contactPhone,
                          @RequestParam(value = "businessLicense", required = false) String businessLicense,
                          RedirectAttributes redirectAttributes) {

        try {
            // 检查用户名是否已存在
            if (userService.getUserByUsername(username) != null) {
                redirectAttributes.addFlashAttribute("error", "用户名已存在！");
                return "redirect:/toRegisterPage";
            }

            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(password);  // 会在service层加密
            user.setEmail(email);

            if ("SELLER".equals(registerType)) {
                user.setRole("SELLER");
                user.setCompanyName(companyName);
                user.setContactPhone(contactPhone);
                user.setBusinessLicense(businessLicense);
            } else {
                user.setRole("USER");
            }

            boolean success = userService.addUser(user);
            if (success) {
                String roleDesc = "SELLER".equals(registerType) ? "卖家" : "普通用户";
                redirectAttributes.addFlashAttribute("success", roleDesc + "注册成功！请登录");
                return "redirect:/toLoginPage?registerSuccess=true";
            } else {
                redirectAttributes.addFlashAttribute("error", "注册失败，请重试");
                return "redirect:/toRegisterPage";
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "注册失败：" + e.getMessage());
            return "redirect:/toRegisterPage";
        }
    }
}