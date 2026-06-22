package com.example.yy25gbs35_goods.config;

import com.example.yy25gbs35_goods.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理静态资源未找到异常（如Chrome DevTools自动请求）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResourceFoundException(NoResourceFoundException e) {
        // 忽略Chrome DevTools等浏览器自动请求的资源
        String resourceName = e.getResourcePath();
        if (resourceName != null && (
            resourceName.contains(".well-known") ||
            resourceName.contains("favicon.ico") ||
            resourceName.contains("robots.txt")
        )) {
            log.debug("忽略浏览器自动请求: {}", resourceName);
            return; // 静默处理，不返回任何内容
        }
        log.warn("静态资源未找到: {}", resourceName);
        // 其他资源未找到也静默处理（避免日志刷屏）
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDeniedException(AccessDeniedException e, Model model) {
        log.warn("访问被拒绝: {}", e.getMessage());
        
        // 如果是API请求，返回JSON
        if (isApiRequest()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "没有权限执行此操作");
            return result;
        }
        
        model.addAttribute("errorCode", 403);
        model.addAttribute("errorMessage", "抱歉，您没有权限访问该页面");
        model.addAttribute("errorDetail", e.getMessage());
        return "error/403";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Object handleNotFoundException(NoHandlerFoundException e, Model model) {
        log.warn("资源未找到: {}", e.getRequestURL());
        
        // 如果是API请求，返回JSON
        if (isApiRequest()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "请求的资源不存在");
            return result;
        }
        
        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", "抱歉，您访问的页面不存在");
        model.addAttribute("errorDetail", e.getRequestURL());
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, Model model) {
        log.error("系统异常: ", e);
        
        // 如果是API请求，返回JSON
        if (isApiRequest()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "系统错误: " + e.getMessage());
            return result;
        }
        
        model.addAttribute("errorCode", 500);
        model.addAttribute("errorMessage", "服务器内部错误，请稍后重试");
        model.addAttribute("errorDetail", e.getMessage());
        return "error/500";
    }

    @ExceptionHandler(RuntimeException.class)
    public Object handleRuntimeException(RuntimeException e, Model model) {
        log.error("运行时异常: ", e);
        
        // 如果是API请求，返回JSON
        if (isApiRequest()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "操作失败: " + e.getMessage());
            return result;
        }
        
        model.addAttribute("errorCode", 500);
        model.addAttribute("errorMessage", "操作失败，请稍后重试");
        model.addAttribute("errorDetail", e.getMessage());
        return "error/500";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(IllegalArgumentException e, Model model) {
        log.warn("参数错误: {}", e.getMessage());
        
        // 如果是API请求，返回JSON
        if (isApiRequest()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "参数错误: " + e.getMessage());
            return result;
        }
        
        model.addAttribute("errorCode", 400);
        model.addAttribute("errorMessage", "请求参数错误");
        model.addAttribute("errorDetail", e.getMessage());
        return "error/400";
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException e, Model model) {
        log.warn("业务异常: {}", e.getMessage());
        
        // 如果是API请求，返回JSON
        if (isApiRequest()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
        
        model.addAttribute("errorCode", e.getCode());
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("errorDetail", null);
        return "error/error";
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, Model model) {
        log.error("参数类型不匹配: ", e);
        
        String paramName = e.getName();
        Object value = e.getValue();
        Class<?> requiredType = e.getRequiredType();
        
        String errorMsg = String.format(
            "参数 '%s' 的值 '%s' 无法转换为 %s",
            paramName != null ? paramName : "未知参数",
            value != null ? value.toString() : "null",
            requiredType != null ? requiredType.getSimpleName() : "未知类型"
        );
        
        log.error("详细错误信息: {}", errorMsg);
        
        // 如果是API请求，返回JSON
        if (isApiRequest()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "参数格式错误: " + errorMsg);
            return result;
        }
        
        model.addAttribute("errorCode", 400);
        model.addAttribute("errorMessage", "请求参数格式错误");
        model.addAttribute("errorDetail", errorMsg);
        return "error/400";
    }
    
    /**
     * 判断是否为API请求（返回JSON而非HTML页面）
     */
    private boolean isApiRequest() {
        try {
            org.springframework.web.context.request.RequestAttributes attrs =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();

            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                HttpServletRequest request =
                    ((org.springframework.web.context.request.ServletRequestAttributes) attrs).getRequest();

                String acceptHeader = request.getHeader("Accept");
                String uri = request.getRequestURI();
                String xhrHeader = request.getHeader("X-Requested-With");

                // 判断条件：
                // 1. Accept头包含application/json
                // 2. 请求路径包含/api/或/admin/orders/等API路径
                // 3. AJAX请求（X-Requested-With: XMLHttpRequest）
                boolean isJsonRequest = acceptHeader != null && acceptHeader.contains("application/json");
                boolean isApiPath = uri.contains("/api/") ||
                                   uri.contains("/admin/orders/") ||
                                   uri.contains("/order/");
                boolean isAjax = "XMLHttpRequest".equals(xhrHeader);

                return isJsonRequest || isApiPath || isAjax;
            }
        } catch (Exception ex) {
            log.debug("判断请求类型时出错: {}", ex.getMessage());
        }
        return false;
    }
}