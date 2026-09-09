package com.example.goods.controller;

import com.example.goods.common.JwtUtil;
import com.example.goods.common.Result;
import com.example.goods.dto.JwtResponse;
import com.example.goods.dto.LoginRequest;
import com.example.goods.goods.User;
import com.example.goods.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<JwtResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            logger.info("🔐 用户登录请求: username={}", loginRequest.getUsername());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.info("✅ 认证成功: username={}", loginRequest.getUsername());

            User user = userService.getUserByUsername(loginRequest.getUsername());
            
            if (user == null) {
                logger.error("❌ 用户不存在: {}", loginRequest.getUsername());
                return Result.error("用户不存在");
            }

            String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());
            logger.info("🎫 Token 生成成功");

            JwtResponse jwtResponse = new JwtResponse(token, "Bearer", user.getId(), user.getUsername(), user.getRole());
            return Result.success("登录成功", jwtResponse);
            
        } catch (BadCredentialsException e) {
            logger.error("❌ 密码错误: {}", e.getMessage());
            return Result.error("用户名或密码错误");
        } catch (Exception e) {
            logger.error("💥 登录失败: {}", e.getMessage(), e);
            return Result.error("登录失败：" + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<String> logout() {
        SecurityContextHolder.clearContext();
        return Result.success("退出成功");
    }

    @GetMapping("/user-info")
    public Result<User> getUserInfo() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.getUserByUsername(username);
            
            if (user != null) {
                user.setPasswordHash(null);  // 清除敏感信息
                return Result.success(user);
            }
            return Result.error("用户不存在");
        } catch (Exception e) {
            logger.error("获取用户信息失败: {}", e.getMessage());
            return Result.error("获取用户信息失败");
        }
    }
}