package com.example.yy25gbs35_goods.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.yy25gbs35_goods.goods.User;
import com.example.yy25gbs35_goods.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class UserContextInterceptor implements HandlerInterceptor {
    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        HttpSession session = request.getSession();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String username = auth.getName();

            User user = (User) session.getAttribute("currentUser");
            if (user == null || !username.equals(user.getUsername())) {
                user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
                if (user != null) {
                    session.setAttribute("currentUser", user);
                    session.setAttribute("username", user.getUsername());
                    session.setAttribute("userAvatar", user.getAvatar());
                    session.setAttribute("userRole", user.getRole());
                    session.setAttribute("userId", user.getId());
                }
            }
        } else {
            session.removeAttribute("currentUser");
            session.removeAttribute("username");
            session.removeAttribute("userAvatar");
            session.removeAttribute("userRole");
            session.removeAttribute("userId");
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                modelAndView.addObject("currentUser", session.getAttribute("currentUser"));
                modelAndView.addObject("username", session.getAttribute("username"));
                modelAndView.addObject("userAvatar", session.getAttribute("userAvatar"));
                modelAndView.addObject("userRole", session.getAttribute("userRole"));
            }
        }
    }
}
