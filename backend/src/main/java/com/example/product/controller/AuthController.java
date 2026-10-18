package com.example.product.controller;

import com.example.product.common.Result;
import com.example.product.dto.LoginRequest;
import com.example.product.dto.LoginResponse;
import com.example.product.security.JwtUtil;
import com.example.product.security.LoginUser;
import com.example.product.security.SecurityUtils;
import com.example.product.service.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录认证接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /** 登录：校验密码 -> 签发 JWT -> 登录态写入 Redis */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success("登录成功", authService.login(request));
    }

    /** 退出登录：删除 Redis 中的登录态，token 立即失效 */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String jti = currentJti(request);
        boolean removed = authService.logout(jti);
        log.info("退出登录, jti={}, redis删除={}", jti, removed);
        return Result.success("退出登录成功", null);
    }

    /** 当前登录用户信息（含角色与权限，前端据此渲染菜单与按钮） */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", loginUser.getUserId());
        data.put("username", loginUser.getUsername());
        data.put("nickname", loginUser.getNickname());
        data.put("email", loginUser.getEmail());
        data.put("roles", loginUser.getRoles());
        data.put("permissions", loginUser.getPermissions());
        return Result.success(data);
    }

    /** 在线会话数（Redis 中的登录态数量），用于演示登录状态确实存在 Redis */
    @GetMapping("/online")
    public Result<Map<String, Object>> online() {
        return Result.success(Map.of("onlineSessions", authService.onlineCount()));
    }

    private String currentJti(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parse(header.substring(7).trim());
                return claims.getId();
            } catch (Exception e) {
                log.debug("解析 token 失败: {}", e.getMessage());
            }
        }
        return null;
    }
}
