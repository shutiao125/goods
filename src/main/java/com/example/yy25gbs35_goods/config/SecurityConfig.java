package com.example.yy25gbs35_goods.config;


import com.example.yy25gbs35_goods.service.UserService;
import com.example.yy25gbs35_goods.service.RememberMeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private RememberMeService rememberMeService;

    public SecurityConfig(UserService userService, DataSource dataSource) {
        this.userService = userService;
        this.dataSource = dataSource;
    }

    /**
     * 密码编码器：使用 BCrypt 加密算法
     * BCrypt 是单向哈希加密，每次生成的密文都不同，但验证结果一致
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器：负责处理身份验证逻辑
     * DaoAuthenticationProvider 从数据库加载用户信息进行比对
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    /**
     * 记住我令牌仓库：使用自定义的 remember_me 表持久化令牌
     */
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        return new PersistentTokenRepository() {
            @Override
            public void createNewToken(PersistentRememberMeToken token) {
                // 直接保存 Spring Security 传入的 series 和 tokenValue
                rememberMeService.saveRememberMe(
                        token.getUsername(), 
                        token.getSeries(),   // 使用原始的 series
                        token.getTokenValue(), // 使用原始的 tokenValue
                        7
                );
            }

            @Override
            public void updateToken(String series, String tokenValue, Date lastUsed) {
                // 更新指定 series 的 token 值
                rememberMeService.updateToken(series, tokenValue);
            }

            @Override
            public PersistentRememberMeToken getTokenForSeries(String seriesId) {
                // 通过 series 查找记录
                com.example.yy25gbs35_goods.goods.RememberMe rememberMe = rememberMeService.getBySeries(seriesId);
                if (rememberMe == null) {
                    return null;
                }
                return new PersistentRememberMeToken(
                        rememberMe.getUsername(),
                        rememberMe.getSeries(),
                        rememberMe.getToken(),
                        java.sql.Timestamp.valueOf(rememberMe.getLastUsed() != null ? rememberMe.getLastUsed() : rememberMe.getCreateTime())
                );
            }

            @Override
            public void removeUserTokens(String username) {
                rememberMeService.invalidateUserTokens(username);
            }
        };
    }

    /**
     * 自定义记住我服务
     */
    @Bean
    public PersistentTokenBasedRememberMeServices rememberMeServices() {
        PersistentTokenBasedRememberMeServices rememberMeServices =
                new PersistentTokenBasedRememberMeServices(
                        UUID.randomUUID().toString(), // 随机密钥
                        userService,
                        persistentTokenRepository());
        rememberMeServices.setParameter("remember-me"); // 参数名
        rememberMeServices.setTokenValiditySeconds(60 * 60 * 24 * 7); // 有效期7天
        return rememberMeServices;
    }

    /**
     * 清理记住我令牌的退出登录处理器：物理删除数据库中的remember_me记录
     */
    @Bean
    public org.springframework.security.web.authentication.logout.LogoutHandler clearRememberMeHandler() {
        return new org.springframework.security.web.authentication.logout.LogoutHandler() {
            @Override
            public void logout(HttpServletRequest request,
                              HttpServletResponse response,
                              Authentication authentication) {
                System.out.println("\n========== 退出登录：清理记住我令牌 ==========");
                System.out.println("🚪 [LogoutHandler] 用户正在退出登录...");
                System.out.println("👤 [LogoutHandler] Authentication对象: " + (authentication != null ? "存在" : "null"));

                try {
                    if (authentication != null && authentication.getName() != null) {
                        String username = authentication.getName();
                        System.out.println("🧹 [LogoutHandler] 开始物理删除用户 " + username + " 的记住我令牌");

                        // 统计删除前的记录数
                        long countBefore = rememberMeService.countByUsername(username);
                        System.out.println("📊 [LogoutHandler] 删除前该用户的记住我记录数: " + countBefore);

                        // 执行物理删除操作
                        rememberMeService.invalidateUserTokens(username);
                        System.out.println("✅ [LogoutHandler] 物理删除操作已执行 (DELETE FROM)");

                        // 统计删除后的记录数
                        long countAfter = rememberMeService.countActiveByUsername(username);
                        System.out.println("📊 [LogoutHandler] 删除后该用户的记住我记录数: " + countAfter);
                        System.out.println("🗑️ [LogoutHandler] 实际删除了 " + (countBefore - countAfter) + " 条记录");

                        if (countBefore > 0 && countAfter == 0) {
                            System.out.println("✅ [LogoutHandler] 记住我令牌已成功从数据库中彻底删除");
                        } else if (countBefore == 0) {
                            System.out.println("ℹ️ [LogoutHandler] 该用户没有需要删除的记住我记录");
                        } else {
                            System.err.println("⚠️ [LogoutHandler] 警告：删除后仍有 " + countAfter + " 条记录未删除");
                        }
                    } else {
                        System.out.println("⚠️ [LogoutHandler] 无法获取用户信息（Authentication为空或name为空）");
                    }
                } catch (Exception e) {
                    System.err.println("❌ [LogoutHandler] 清理记住我令牌失败:");
                    e.printStackTrace();
                }
                System.out.println("=====================================\n");
            }
        };
    }

    /**
     * 自定义登录成功处理器：根据角色跳转不同页面
     */
    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

                if (roles.contains("ROLE_ADMIN")) {
                    // 管理员 → 管理后台
                    response.sendRedirect("/admin/dashboard");
                } else if (roles.contains("ROLE_SELLER")) {
                    // 卖家 → 卖家后台
                    response.sendRedirect("/seller/dashboard");
                } else {
                    // 普通用户 → 文章列表页面
                    response.sendRedirect("/articles");
                }
            }
        };
    }

    /**
     * 安全过滤链配置：定义基于角色的访问控制规则
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(cookieCsrfTokenRepository())
                        .ignoringRequestMatchers("/toLoginPage", "/login", "/logout", "/goods/batch-delete", "/goods/select-local-image",
                                "/cart/add", "/cart/update/*", "/cart/delete/*", "/cart/count",
                                "/order/**",
                                "/admin/orders/**",
                                "/admin/comments/**", "/address/**",
                                "/admin/users/**",
                                "/admin/articles/delete/**",
                                "/articles/create",
                                "/articles/update/**",
                                "/articles/*/update-goods",
                                "/articles/*/comments/**",
                                "/articles/batch-delete",
                                "/articles/batch-download",
                                "/seller/**")
                )
                // 授权规则配置：基于角色的访问控制
                .authorizeHttpRequests(auth -> auth
                        // 只允许匿名访问登录相关资源和静态资源
                        .requestMatchers("/toLoginPage", "/toRegisterPage", "/login", "/logout", "/register").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/images/**", "/static/**", "/files/static/**", "/uploads/**").permitAll()
                        .requestMatchers("/*.ico", "/*.png", "/*.jpg", "/*.jpeg", "/*.gif").permitAll()
                        // 编辑者和管理员可访问文章管理（优先）
                        .requestMatchers("/admin/articles/**").hasAnyRole("ADMIN", "EDITOR")
                        // 管理员专属路径：只有 ROLE_ADMIN 可访问
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 卖家专属路径：只有 ROLE_SELLER 可访问
                        .requestMatchers("/seller/**").hasRole("SELLER")
                        // 其他所有请求需要认证
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 未认证用户访问受保护资源时重定向到登录页面
                            response.sendRedirect("/toLoginPage");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // 已认证但权限不足的用户显示403错误
                            response.sendError(403, "Forbidden");
                        })
                )
                .formLogin(form -> form
                        .loginPage("/toLoginPage")
                        .loginProcessingUrl("/login")
                        .successHandler(customAuthenticationSuccessHandler())
                        .failureUrl("/toLoginPage?error=true")
                        .permitAll()
                )
                .rememberMe(rememberMe -> rememberMe
                        .tokenRepository(persistentTokenRepository())
                        .tokenValiditySeconds(60 * 60 * 24 * 7) // 7天有效期
                        .userDetailsService(userService)
                        .rememberMeParameter("remember-me")
                        .rememberMeServices(rememberMeServices())
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .addLogoutHandler(clearRememberMeHandler())
                        .logoutSuccessUrl("/toLoginPage?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * 使用 Cookie 存储 CSRF token，避免创建 session
     */
    @Bean
    public CsrfTokenRepository cookieCsrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }
}