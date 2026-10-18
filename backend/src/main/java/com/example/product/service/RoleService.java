package com.example.product.service;

import com.example.product.common.BusinessException;
import com.example.product.common.PageResult;
import com.example.product.common.ResultCode;
import com.example.product.dto.AssignRequest;
import com.example.product.dto.RoleSaveRequest;
import com.example.product.dto.RoleVO;
import com.example.product.entity.SysPermission;
import com.example.product.entity.SysRole;
import com.example.product.repository.SysPermissionRepository;
import com.example.product.repository.SysRoleRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 角色管理（CRUD + 分页 + 分配权限）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleRepository roleRepository;
    private final SysPermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public PageResult<RoleVO> page(int page, int size, String keyword, Integer status) {
        Specification<SysRole> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("name")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<SysRole> result = roleRepository.findAll(spec,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.ASC, "sortOrder", "id")));
        return PageResult.of(result, RoleVO::from);
    }

    @Transactional(readOnly = true)
    public List<RoleVO> listAll() {
        return roleRepository.findAllWithPermissions().stream().map(RoleVO::from).toList();
    }

    @Transactional(readOnly = true)
    public RoleVO getById(Long id) {
        SysRole role = roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "角色不存在: " + id));
        return RoleVO.from(role);
    }

    @Transactional
    public RoleVO create(RoleSaveRequest request) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new BusinessException(ResultCode.CONFLICT, "角色标识已存在: " + request.getCode());
        }
        SysRole role = new SysRole();
        role.setCode(request.getCode());
        applyFields(role, request);
        role.setPermissions(resolvePermissions(request.getPermissionIds()));
        SysRole saved = roleRepository.save(role);
        log.info("新增角色: {} ({})", saved.getName(), saved.getCode());
        return RoleVO.from(saved);
    }

    @Transactional
    public RoleVO update(Long id, RoleSaveRequest request) {
        SysRole role = roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "角色不存在: " + id));
        if (roleRepository.existsByCodeAndIdNot(request.getCode(), id)) {
            throw new BusinessException(ResultCode.CONFLICT, "角色标识已被占用: " + request.getCode());
        }
        role.setCode(request.getCode());
        applyFields(role, request);
        role.setPermissions(resolvePermissions(request.getPermissionIds()));
        SysRole saved = roleRepository.save(role);
        log.info("修改角色: {}", saved.getCode());
        return RoleVO.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "角色不存在: " + id));
        if ("ADMIN".equals(role.getCode())) {
            throw new BusinessException("内置超级管理员角色不允许删除");
        }
        // 先解除角色-权限关联，再删除角色，避免外键约束
        role.getPermissions().clear();
        roleRepository.save(role);
        roleRepository.delete(role);
        log.info("删除角色: {}", role.getCode());
    }

    /** 给角色分配权限 */
    @Transactional
    public RoleVO assignPermissions(AssignRequest request) {
        SysRole role = roleRepository.findWithPermissionsById(request.getId())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "角色不存在: " + request.getId()));
        role.setPermissions(resolvePermissions(request.safeIds()));
        SysRole saved = roleRepository.save(role);
        log.info("角色 {} 分配权限数量: {}", saved.getCode(), saved.getPermissions().size());
        return RoleVO.from(saved);
    }

    private void applyFields(SysRole role, RoleSaveRequest request) {
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        role.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    private Set<SysPermission> resolvePermissions(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        List<SysPermission> permissions = permissionRepository.findByIdIn(permissionIds);
        if (permissions.size() != permissionIds.stream().distinct().count()) {
            throw new BusinessException("存在无效的权限 id");
        }
        return new HashSet<>(permissions);
    }
}
