package com.example.goods.service;

import com.example.goods.goods.RememberMe;

public interface RememberMeService {

    /**
     * 保存记住我信息
     */
    void saveRememberMe(String username, String series, String token, int days);

    /**
     * 根据令牌获取记住我信息
     */
    RememberMe getByToken(String token);

    /**
     * 根据 series 获取记住我信息（Spring Security 使用）
     */
    RememberMe getBySeries(String series);

    /**
     * 更新令牌（Spring Security 使用）
     */
    void updateToken(String series, String newToken);

    /**
     * 删除过期的记住我信息
     */
    void deleteExpired();

    /**
     * 使令牌失效
     */
    void invalidateToken(String token);

    /**
     * 使 series 失效（Spring Security 使用）
     */
    void invalidateSeries(String series);

    /**
     * 使用户的所有令牌失效
     */
    void invalidateUserTokens(String username);

    /**
     * 统计用户的记住我记录总数
     */
    long countByUsername(String username);

    /**
     * 统计用户的有效记住我记录数（status=1）
     */
    long countActiveByUsername(String username);
}