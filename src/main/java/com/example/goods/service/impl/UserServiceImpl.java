package com.example.goods.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goods.goods.User;
import com.example.goods.mapper.UserMapper;
import com.example.goods.service.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = this.getOne(wrapper);

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new UsernameNotFoundException("用户状态异常: " + user.getStatus());
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()))
        );
    }

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return this.getOne(wrapper);
    }

    @Override
    public User getUserById(Long id) {
        return this.getById(id);
    }

    @Override
    public Page<User> getUsersByPage(int pageNum, int pageSize, String keyword, String role, String status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(keyword), User::getUsername, keyword)
                .or()
                .like(StrUtil.isNotBlank(keyword), User::getEmail, keyword)
                .eq(StrUtil.isNotBlank(role), User::getRole, role)
                .eq(StrUtil.isNotBlank(status), User::getStatus, status)
                .orderByDesc(User::getCreatedAt);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public boolean addUser(User user) {
        // 密码加密
        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        // 设置默认值
        if (user.getStatus() == null || user.getStatus().isEmpty()) {
            user.setStatus("ACTIVE");
        }
        return this.save(user);
    }

    @Override
    public boolean updateUser(User user) {
        return this.updateById(user);
    }

    @Override
    public boolean deleteUser(Integer id) {
        return this.removeById(id);
    }

    @Override
    public boolean deleteUsers(List<Integer> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public boolean updateUserStatus(Integer id, String status) {
        User user = this.getById(id);
        if (user != null) {
            user.setStatus(status);
            return this.updateById(user);
        }
        return false;
    }

    @Override
    public boolean updateUserRole(Integer id, String role) {
        User user = this.getById(id);
        if (user != null) {
            user.setRole(role);
            return this.updateById(user);
        }
        return false;
    }

    @Override
    public long countByRole(String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, role);
        return this.count(wrapper);
    }
}