package com.example.product.security;

import com.example.product.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 生成与解析。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireSeconds;

    public JwtUtil(AppProperties properties) {
        byte[] bytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expireSeconds = properties.getJwt().getExpireSeconds();
    }

    /** 生成一个新的 jti（同时作为 Redis 登录态 key 的后缀） */
    public String newJti() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    /**
     * 生成 token。jti 会被写入 Redis 白名单，登出时删除即可让 token 立即失效。
     */
    public String generateToken(Long userId, String username, String jti) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireSeconds * 1000L);
        return Jwts.builder()
                .id(jti)
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** 解析并校验签名/过期时间；非法时抛出 JwtException */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
