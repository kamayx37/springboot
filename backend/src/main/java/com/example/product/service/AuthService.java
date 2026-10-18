package com.example.product.service;

import com.example.product.common.BusinessException;
import com.example.product.common.ResultCode;
import com.example.product.dto.LoginRequest;
import com.example.product.dto.LoginResponse;
import com.example.product.entity.SysUser;
import com.example.product.repository.SysUserRepository;
import com.example.product.security.JwtUtil;
import com.example.product.security.LoginUser;
import com.example.product.security.TokenService;
import com.example.product.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录认证：BCrypt 校验密码 -> 生成 JWT -> 登录态写入 Redis。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        SysUser user = userRepository.findWithRolesByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误"));

        // 真实的 BCrypt 密文比对，数据库里不存在明文密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败，密码不匹配: {}", request.getUsername());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        LoginUser loginUser = UserDetailsServiceImpl.toLoginUser(user);
        String jti = jwtUtil.newJti();
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), jti);

        // 登录状态保存到 Redis（key 为 token 的 jti），退出后删除该 key 即可让 token 立即失效
        tokenService.saveLogin(jti, loginUser);
        log.info("用户登录成功: {} (角色: {}, 权限数: {})",
                user.getUsername(), loginUser.getRoles(), loginUser.getPermissions().size());

        return LoginResponse.of(token, jwtUtil.getExpireSeconds(), loginUser);
    }

    /** 退出登录：删除 Redis 中的登录态，token 立即失效 */
    public boolean logout(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return tokenService.removeLogin(jti);
    }

    /** 当前在线会话数 */
    public long onlineCount() {
        return tokenService.countOnline();
    }
}
