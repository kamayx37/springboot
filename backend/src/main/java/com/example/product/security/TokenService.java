package com.example.product.security;

import com.example.product.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * 登录状态管理：token 的 jti 作为 Redis 白名单 key。
 * <p>
 * 登录  -> SET product:login:{jti} = 用户信息 JSON (TTL=token 有效期)
 * 鉴权  -> 校验 JWT 签名后，还必须命中 Redis 白名单
 * 登出  -> DEL product:login:{jti}，token 立即失效
 */
@Slf4j
@Service
public class TokenService {

    public static final String LOGIN_KEY_PREFIX = "product:login:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final long expireSeconds;

    public TokenService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, AppProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.expireSeconds = properties.getJwt().getExpireSeconds();
    }

    /** 登录成功后写入 Redis 白名单 */
    public void saveLogin(String jti, LoginUser loginUser) {
        try {
            String json = objectMapper.writeValueAsString(loginUser);
            redisTemplate.opsForValue().set(LOGIN_KEY_PREFIX + jti, json, Duration.ofSeconds(expireSeconds));
            log.debug("登录态写入 Redis: {}{}", LOGIN_KEY_PREFIX, jti);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("登录用户信息序列化失败", e);
        }
    }

    /** 从 Redis 读取登录态；返回 null 表示未登录 / 已登出 / 已过期 */
    public LoginUser getLogin(String jti) {
        String json = redisTemplate.opsForValue().get(LOGIN_KEY_PREFIX + jti);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LoginUser.class);
        } catch (JsonProcessingException e) {
            log.warn("Redis 中的登录态无法反序列化, jti={}", jti);
            return null;
        }
    }

    /** 续期（活跃用户自动延长登录态） */
    public void renew(String jti) {
        redisTemplate.expire(LOGIN_KEY_PREFIX + jti, Duration.ofSeconds(expireSeconds));
    }

    /** 登出：删除白名单，token 立即失效 */
    public boolean removeLogin(String jti) {
        Boolean removed = redisTemplate.delete(LOGIN_KEY_PREFIX + jti);
        log.debug("登出删除 Redis 登录态: {}{} -> {}", LOGIN_KEY_PREFIX, jti, removed);
        return Boolean.TRUE.equals(removed);
    }

    /** 当前在线会话数 */
    public long countOnline() {
        Set<String> keys = redisTemplate.keys(LOGIN_KEY_PREFIX + "*");
        return keys == null ? 0L : keys.size();
    }

    /** 强制某个用户的所有会话下线（改密码/禁用时使用） */
    public void removeByUserId(Long userId) {
        Set<String> keys = redisTemplate.keys(LOGIN_KEY_PREFIX + "*");
        if (keys == null) {
            return;
        }
        for (String key : keys) {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                continue;
            }
            try {
                LoginUser u = objectMapper.readValue(json, LoginUser.class);
                if (userId.equals(u.getUserId())) {
                    redisTemplate.delete(key);
                }
            } catch (JsonProcessingException ignored) {
                // 忽略脏数据
            }
        }
    }
}
