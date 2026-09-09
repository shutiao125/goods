package com.example.goods.config;

import com.example.goods.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    @Autowired
    private DataSource dataSource;

    public SecurityConfig(UserService userService, DataSource dataSource) {
        this.userService = userService;
        this.dataSource = dataSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 公开 API - 无需认证
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/goods/**", "/api/articles/**", "/api/public/**").permitAll()
                        .requestMatchers("/api/cart/**").permitAll()  // 购物车暂时开放，后续可加认证
                        
                        // 静态资源 - 无需认证
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/images/**").permitAll()
                        .requestMatchers("/static/**", "/files/static/**", "/uploads/**").permitAll()
                        .requestMatchers("/*.ico", "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.svg").permitAll()
                        
                        // 旧版页面路由 - 兼容性保留（可选）
                        .requestMatchers("/toLoginPage", "/toRegisterPage", "/login", "/logout", "/register").permitAll()
                        .requestMatchers("/goods/list", "/articles", "/goods/*").permitAll()
                        
                        // 错误页面
                        .requestMatchers("/error", "/error/**").permitAll()
                        
                        // 根路径和健康检查
                        .requestMatchers("/", "/health", "/actuator/**").permitAll()
                        
                        // Swagger/API 文档（如果使用）
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        
                        // 管理员专属路径
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        
                        // 卖家专属路径
                        .requestMatchers("/api/seller/**").hasRole("SELLER")
                        
                        // 其他所有请求需要认证
                        .anyRequest().authenticated()
                )
                // 添加 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                
                // 异常处理：未认证返回 401 JSON 而不是重定向
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":401,\"message\":\"未登录或 Token 已过期\",\"data\":null}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":403,\"message\":\"权限不足\",\"data\":null}");
                        })
                );

        return http.build();
    }
}