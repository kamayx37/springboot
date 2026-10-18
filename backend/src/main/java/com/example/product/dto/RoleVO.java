package com.example.product.dto;

import com.example.product.entity.SysRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色列表 / 详情响应。
 */
@Data
public class RoleVO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private List<Long> permissionIds;
    private List<String> permissionCodes;
    private Integer userCount;

    public static RoleVO from(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setSortOrder(role.getSortOrder());
        vo.setCreatedAt(role.getCreatedAt());
        vo.setPermissionIds(role.getPermissions().stream().map(p -> p.getId()).sorted().toList());
        vo.setPermissionCodes(role.getPermissions().stream().map(p -> p.getCode()).sorted().toList());
        return vo;
    }
}
