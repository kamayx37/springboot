package com.example.product.repository;

import com.example.product.entity.SysPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface SysPermissionRepository extends JpaRepository<SysPermission, Long>, JpaSpecificationExecutor<SysPermission> {

    Optional<SysPermission> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<SysPermission> findByIdIn(List<Long> ids);

    List<SysPermission> findByParentIdOrderBySortOrderAsc(Long parentId);

    List<SysPermission> findAllByOrderBySortOrderAscIdAsc();
}
