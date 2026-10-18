package com.example.product.security;

import com.example.product.entity.SysPermission;
import com.example.product.entity.SysUser;
import com.example.product.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 从数据库加载用户、角色、权限，构建登录上下文。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        return toLoginUser(user);
    }

    /** 把实体转换成登录上下文（含角色码与权限码） */
    public static LoginUser toLoginUser(SysUser user) {
        List<String> roles = user.getRoles().stream()
                .filter(r -> r.getStatus() == null || r.getStatus() == 1)
                .map(r -> r.getCode())
                .distinct()
                .collect(Collectors.toList());

        List<String> permissions = user.getRoles().stream()
                .filter(r -> r.getStatus() == null || r.getStatus() == 1)
                .flatMap(r -> r.getPermissions().stream())
                .map(SysPermission::getCode)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setEmail(user.getEmail());
        loginUser.setPassword(user.getPassword());
        loginUser.setStatus(user.getStatus());
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        return loginUser;
    }
}
