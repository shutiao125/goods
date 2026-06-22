package com.example.yy25gbs35_goods.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yy25gbs35_goods.goods.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends IService<User>, UserDetailsService {

    User getUserByUsername(String username);
    
    User getUserById(Long id);

    Page<User> getUsersByPage(int pageNum, int pageSize, String keyword, String role, String status);

    boolean addUser(User user);

    boolean updateUser(User user);

    boolean deleteUser(Integer id);

    boolean deleteUsers(List<Integer> ids);

    boolean updateUserStatus(Integer id, String status);

    boolean updateUserRole(Integer id, String role);
    
    long countByRole(String role);
}