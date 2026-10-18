package com.example.product.service;

import com.example.product.common.BusinessException;
import com.example.product.common.PageResult;
import com.example.product.common.ResultCode;
import com.example.product.dto.PermissionSaveRequest;
import com.example.product.dto.PermissionVO;
import com.example.product.entity.SysPermission;
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
import java.util.List;

/**
 * 权限管理（CRUD + 树形结构）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysPermissionRepository permissionRepository;
    private final SysRoleRepository roleRepository;

    /** 树形全量列表（用于角色分配权限的树控件） */
    @Transactional(readOnly = true)
    public List<PermissionVO> tree() {
        return PermissionVO.toTree(permissionRepository.findAllByOrderBySortOrderAscIdAsc());
    }

    @Transactional(readOnly = true)
    public PageResult<PermissionVO> page(int page, int size, String keyword, String type) {
        Specification<SysPermission> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("name")), like)));
            }
            if (StringUtils.hasText(type)) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<SysPermission> result = permissionRepository.findAll(spec,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.ASC, "sortOrder", "id")));
        return PageResult.of(result, PermissionVO::from);
    }

    @Transactional(readOnly = true)
    public List<PermissionVO> listAll() {
        return permissionRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(PermissionVO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermissionVO getById(Long id) {
        return PermissionVO.from(findPermission(id));
    }

    @Transactional
    public PermissionVO create(PermissionSaveRequest request) {
        if (permissionRepository.existsByCode(request.getCode())) {
            throw new BusinessException(ResultCode.CONFLICT, "权限标识已存在: " + request.getCode());
        }
        if (request.getParentId() != null && request.getParentId() != 0L) {
            findPermission(request.getParentId());
        }
        SysPermission entity = request.toEntity();
        SysPermission saved = permissionRepository.save(entity);
        log.info("新增权限: {} - {}", saved.getCode(), saved.getName());
        return PermissionVO.from(saved);
    }

    @Transactional
    public PermissionVO update(Long id, PermissionSaveRequest request) {
        SysPermission entity = findPermission(id);
        if (permissionRepository.existsByCodeAndIdNot(request.getCode(), id)) {
            throw new BusinessException(ResultCode.CONFLICT, "权限标识已被占用: " + request.getCode());
        }
        if (request.getParentId() != null && request.getParentId().equals(id)) {
            throw new BusinessException("上级权限不能是自己");
        }
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setType(request.getType() == null ? "BUTTON" : request.getType());
        entity.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        entity.setPath(request.getPath());
        entity.setIcon(request.getIcon());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setDescription(request.getDescription());
        SysPermission saved = permissionRepository.save(entity);
        log.info("修改权限: {}", saved.getCode());
        return PermissionVO.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        SysPermission entity = findPermission(id);
        if (!permissionRepository.findByParentIdOrderBySortOrderAsc(id).isEmpty()) {
            throw new BusinessException("请先删除该权限下的子权限");
        }
        // 解除与角色的关联
        roleRepository.findAllWithPermissions().forEach(role -> {
            boolean removed = role.getPermissions().removeIf(p -> p.getId().equals(id));
            if (removed) {
                roleRepository.save(role);
            }
        });
        permissionRepository.delete(entity);
        log.info("删除权限: {}", entity.getCode());
    }

    private SysPermission findPermission(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "权限不存在: " + id));
    }
}
