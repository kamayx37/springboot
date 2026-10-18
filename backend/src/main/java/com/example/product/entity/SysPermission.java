package com.example.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 权限（菜单 / 按钮 / 接口）。
 */
@Getter
@Setter
@Entity
@Table(name = "sys_permission", indexes = {
        @Index(name = "idx_sys_permission_code", columnList = "code", unique = true)
})
public class SysPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 权限标识，如 product:create */
    @Column(nullable = false, unique = true, length = 128)
    private String code;

    /** 权限名称 */
    @Column(nullable = false, length = 64)
    private String name;

    /** MENU / BUTTON / API */
    @Column(nullable = false, length = 16)
    private String type = "BUTTON";

    /** 父权限 id，顶级为 0 */
    @Column(name = "parent_id")
    private Long parentId = 0L;

    /** 前端路由路径（type=MENU 时使用） */
    @Column(length = 128)
    private String path;

    @Column(length = 64)
    private String icon;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.parentId == null) {
            this.parentId = 0L;
        }
    }
}
