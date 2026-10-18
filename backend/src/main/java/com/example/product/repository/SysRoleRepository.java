package com.example.product.repository;

import com.example.product.entity.SysRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SysRoleRepository extends JpaRepository<SysRole, Long>, JpaSpecificationExecutor<SysRole> {

    Optional<SysRole> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<SysRole> findByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = {"permissions"})
    @Query("select r from SysRole r where r.id = :id")
    Optional<SysRole> findWithPermissionsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"permissions"})
    @Query("select distinct r from SysRole r")
    List<SysRole> findAllWithPermissions();
}
