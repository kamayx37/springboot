package com.example.product.repository;

import com.example.product.entity.SysUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long>, JpaSpecificationExecutor<SysUser> {

    Optional<SysUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    /** 登录时一次性把角色与权限抓出来，避免懒加载异常 */
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query("select u from SysUser u where u.username = :username")
    Optional<SysUser> findWithRolesByUsername(@Param("username") String username);

    @EntityGraph(attributePaths = {"roles"})
    @Query("select u from SysUser u where u.id = :id")
    Optional<SysUser> findWithRolesById(@Param("id") Long id);
}
