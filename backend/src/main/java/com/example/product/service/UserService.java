package com.example.product.service;

import com.example.product.common.BusinessException;
import com.example.product.common.PageResult;
import com.example.product.common.ResultCode;
import com.example.product.dto.AssignRequest;
import com.example.product.dto.UserSaveRequest;
import com.example.product.dto.UserVO;
import com.example.product.entity.SysRole;
import com.example.product.entity.SysUser;
import com.example.product.repository.SysRoleRepository;
import com.example.product.repository.SysUserRepository;
import com.example.product.security.TokenService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户管理（CRUD + 分页 + 分配角色 + 重置密码）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public PageResult<UserVO> page(int page, int size, String keyword, Integer status) {
        Specification<SysUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("nickname")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<SysUser> result = userRepository.findAll(spec,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.of(result, UserVO::from);
    }

    @Transactional(readOnly = true)
    public UserVO getById(Long id) {
        return UserVO.from(findUser(id));
    }

    @Transactional(readOnly = true)
    public List<UserVO> listAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(UserVO::from)
                .toList();
    }

    @Transactional
    public UserVO create(UserSaveRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在: " + request.getUsername());
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("新增用户时密码不能为空");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        // BCrypt 加密存储，绝不保存明文
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        applyBasicFields(user, request);
        user.setRoles(resolveRoles(request.getRoleIds()));
        SysUser saved = userRepository.save(user);
        log.info("新增用户: {}", saved.getUsername());
        return UserVO.from(saved);
    }

    @Transactional
    public UserVO update(Long id, UserSaveRequest request) {
        SysUser user = findUser(id);
        if (userRepository.existsByUsernameAndIdNot(request.getUsername(), id)) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已被占用: " + request.getUsername());
        }
        user.setUsername(request.getUsername());
        // 密码留空表示不修改
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            // 改密码后强制该用户所有会话下线
            tokenService.removeByUserId(id);
        }
        applyBasicFields(user, request);
        user.setRoles(resolveRoles(request.getRoleIds()));
        SysUser saved = userRepository.save(user);
        log.info("修改用户: {}", saved.getUsername());
        return UserVO.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        SysUser user = findUser(id);
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("内置超级管理员不允许删除");
        }
        userRepository.delete(user);
        tokenService.removeByUserId(id);
        log.info("删除用户: {}", user.getUsername());
    }

    /** 给用户分配角色 */
    @Transactional
    public UserVO assignRoles(AssignRequest request) {
        SysUser user = findUser(request.getId());
        user.setRoles(resolveRoles(request.safeIds()));
        SysUser saved = userRepository.save(user);
        // 权限变更后让该用户的旧会话失效，下次请求将重新加载权限
        tokenService.removeByUserId(saved.getId());
        log.info("用户 {} 分配角色: {}", saved.getUsername(), request.safeIds());
        return UserVO.from(saved);
    }

    /** 重置密码 */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 4) {
            throw new BusinessException("新密码长度至少 4 位");
        }
        SysUser user = findUser(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenService.removeByUserId(id);
        log.info("重置用户密码: {}", user.getUsername());
    }

    /** 修改自己的密码 */
    @Transactional
    public void changeOwnPassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = findUser(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 4) {
            throw new BusinessException("新密码长度至少 4 位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenService.removeByUserId(userId);
        log.info("用户修改密码: {}", user.getUsername());
    }

    private void applyBasicFields(SysUser user, UserSaveRequest request) {
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        user.setRemark(request.getRemark());
    }

    private Set<SysRole> resolveRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        List<SysRole> roles = roleRepository.findByIdIn(roleIds);
        if (roles.size() != roleIds.stream().distinct().count()) {
            throw new BusinessException("存在无效的角色 id");
        }
        return new HashSet<>(roles);
    }

    private SysUser findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在: " + id));
    }
}
