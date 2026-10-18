package com.example.product.security;

import com.example.product.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * JWT 认证过滤器。
 * <p>
 * 完整链路：解析 Authorization 头 -> 校验 JWT 签名与过期 -> 校验 Redis 白名单中的 jti ->
 * 构建 SecurityContext。任一环节失败都不会建立认证，最终由 EntryPoint 返回 401。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final StringRedisTemplate redisTemplate;
    private final AppProperties properties;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtUtil.parse(token);
                String jti = claims.getId();
                LoginUser loginUser = tokenService.getLogin(jti);
                if (loginUser != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    renewIfNeeded(jti);
                } else {
                    log.debug("token 有效但 Redis 中无登录态（已登出或过期）, jti={}", jti);
                }
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("JWT 校验失败: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    /** 仅在剩余有效期不足一半时续期，避免每个请求都写 Redis */
    private void renewIfNeeded(String jti) {
        Long ttl = redisTemplate.getExpire(TokenService.LOGIN_KEY_PREFIX + jti);
        long expireSeconds = jwtUtil.getExpireSeconds();
        if (ttl != null && ttl > 0 && ttl < expireSeconds / 2) {
            redisTemplate.expire(TokenService.LOGIN_KEY_PREFIX + jti, Duration.ofSeconds(expireSeconds));
        }
    }

    /** 从 Authorization: Bearer xxx 中提取 token */
    private String resolveToken(HttpServletRequest request) {
        String header = properties.getJwt().getHeader();
        String prefix = properties.getJwt().getPrefix();
        String value = request.getHeader(header);
        if (StringUtils.hasText(value) && value.startsWith(prefix)) {
            return value.substring(prefix.length()).trim();
        }
        return null;
    }
}
