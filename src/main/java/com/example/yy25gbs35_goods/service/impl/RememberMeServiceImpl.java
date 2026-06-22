package com.example.yy25gbs35_goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.yy25gbs35_goods.goods.RememberMe;
import com.example.yy25gbs35_goods.mapper.RememberMeMapper;
import com.example.yy25gbs35_goods.service.RememberMeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RememberMeServiceImpl implements RememberMeService {

    @Autowired
    private RememberMeMapper rememberMeMapper;

    @Override
    @Transactional
    public void saveRememberMe(String username, String series, String token, int days) {
        RememberMe rememberMe = new RememberMe();
        rememberMe.setUsername(username);
        rememberMe.setPasswordHash("");  // 设置空字符串避免数据库错误
        rememberMe.setSeries(series);
        rememberMe.setToken(token);
        rememberMe.setExpireTime(LocalDateTime.now().plusDays(days));
        rememberMe.setCreateTime(LocalDateTime.now());
        rememberMe.setLastUsed(LocalDateTime.now());
        rememberMe.setStatus(1);

        System.out.println("🔐 [RememberMeService] 保存记住我令牌:");
        System.out.println("   用户名: " + username);
        System.out.println("   Series: " + series);
        System.out.println("   Token: " + token.substring(0, Math.min(10, token.length())) + "...");
        System.out.println("   有效期: " + days + "天");

        rememberMeMapper.insert(rememberMe);

        System.out.println("✅ [RememberMeService] 令牌保存成功");
    }

    @Override
    public RememberMe getByToken(String token) {
        LambdaQueryWrapper<RememberMe> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RememberMe::getToken, token)
                .eq(RememberMe::getStatus, 1)
                .gt(RememberMe::getExpireTime, LocalDateTime.now());

        return rememberMeMapper.selectOne(wrapper);
    }

    @Override
    public RememberMe getBySeries(String series) {
        LambdaQueryWrapper<RememberMe> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RememberMe::getSeries, series)
                .eq(RememberMe::getStatus, 1)
                .gt(RememberMe::getExpireTime, LocalDateTime.now());

        return rememberMeMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public void updateToken(String series, String newToken) {
        LambdaUpdateWrapper<RememberMe> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RememberMe::getSeries, series)
                .set(RememberMe::getToken, newToken)
                .set(RememberMe::getLastUsed, LocalDateTime.now());

        rememberMeMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void deleteExpired() {
        LambdaQueryWrapper<RememberMe> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(RememberMe::getExpireTime, LocalDateTime.now());

        rememberMeMapper.delete(wrapper);
    }

    @Override
    @Transactional
    public void invalidateToken(String token) {
        LambdaUpdateWrapper<RememberMe> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RememberMe::getToken, token)
                .set(RememberMe::getStatus, 0);

        rememberMeMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void invalidateSeries(String series) {
        LambdaUpdateWrapper<RememberMe> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RememberMe::getSeries, series)
                .set(RememberMe::getStatus, 0);

        rememberMeMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void invalidateUserTokens(String username) {
        LambdaQueryWrapper<RememberMe> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RememberMe::getUsername, username);

        rememberMeMapper.delete(wrapper);
    }

    @Override
    public long countByUsername(String username) {
        LambdaQueryWrapper<RememberMe> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RememberMe::getUsername, username);

        return rememberMeMapper.selectCount(wrapper);
    }

    @Override
    public long countActiveByUsername(String username) {
        LambdaQueryWrapper<RememberMe> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RememberMe::getUsername, username);

        return rememberMeMapper.selectCount(wrapper);
    }
}