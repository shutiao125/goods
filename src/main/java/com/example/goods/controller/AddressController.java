package com.example.goods.controller;

import com.example.goods.goods.Address;
import com.example.goods.goods.User;
import com.example.goods.service.AddressService;
import com.example.goods.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @Autowired
    private UserService userService;

    // ==================== 具体路径（必须放在通配符路径之前！）====================

    @GetMapping({"/", "/list"})
    @ResponseBody
    public Map<String, Object> getAddressList() {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                result.put("success", false);
                result.put("message", "未登录");
                return result;
            }
            List<Address> addresses = addressService.getAddressByUserId(userId);
            result.put("success", true);
            result.put("data", addresses != null ? addresses : new java.util.ArrayList<>());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/add")
    @ResponseBody
    public Map<String, Object> addAddress(@RequestBody Address address) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            address.setUserId(userId);
            boolean success = addressService.save(address);
            if (success && address.getIsDefault() != null && address.getIsDefault() == 1) {
                addressService.setDefaultAddress(address.getId(), userId);
            }
            result.put("success", success);
            result.put("message", success ? "添加成功" : "添加失败");
            result.put("data", address);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/update")
    @ResponseBody
    public Map<String, Object> updateAddress(@RequestBody Address address) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            Address existing = addressService.getById(address.getId());
            if (existing == null || !existing.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "地址不存在");
                return result;
            }

            boolean success = addressService.updateById(address);
            if (success && address.getIsDefault() != null && address.getIsDefault() == 1) {
                addressService.setDefaultAddress(address.getId(), userId);
            }
            result.put("success", success);
            result.put("message", success ? "更新成功" : "更新失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> deleteAddress(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            Address address = addressService.getById(id);
            if (address == null || !address.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "地址不存在");
                return result;
            }
            boolean success = addressService.removeById(id);
            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/default/{id}")
    @ResponseBody
    public Map<String, Object> setDefault(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            boolean success = addressService.setDefaultAddress(id, userId);
            result.put("success", success);
            result.put("message", success ? "设置成功" : "设置失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.getUserByUsername(username);
        return user != null ? user.getId() : null;
    }
}