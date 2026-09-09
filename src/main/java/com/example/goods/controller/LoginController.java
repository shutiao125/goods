package com.example.goods.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Calendar;

@Controller
public class LoginController {

    @GetMapping("/toLoginPage")
    public String toLoginPage(
            HttpServletRequest request,
            Model model,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registerSuccess", required = false) String registerSuccess) {

        request.getSession(false);

        model.addAttribute("currentYear", Calendar.getInstance().get(Calendar.YEAR));

        if (error != null) {
            model.addAttribute("errorMsg", "用户名或密码错误！");
        }

        if (logout != null) {
            model.addAttribute("logoutMsg", "已成功退出登录！");
        }

        if (registerSuccess != null) {
            model.addAttribute("successMsg", "注册成功！请使用新账号登录");
        }

        return "login";
    }
}