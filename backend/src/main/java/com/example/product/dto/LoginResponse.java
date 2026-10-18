package com.example.product.dto;

import com.example.product.security.LoginUser;
import lombok.Data;

import java.util.List;

/**
 * 登录响应。
 */
@Data
public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserInfo userInfo;

    @Data
    public static class UserInfo {
        private Long userId;
        private String username;
        private String nickname;
        private String email;
        private List<String> roles;
        private List<String> permissions;
    }

    public static LoginResponse of(String token, long expiresIn, LoginUser loginUser) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresIn(expiresIn);
        UserInfo info = new UserInfo();
        info.setUserId(loginUser.getUserId());
        info.setUsername(loginUser.getUsername());
        info.setNickname(loginUser.getNickname());
        info.setEmail(loginUser.getEmail());
        info.setRoles(loginUser.getRoles());
        info.setPermissions(loginUser.getPermissions());
        response.setUserInfo(info);
        return response;
    }
}
