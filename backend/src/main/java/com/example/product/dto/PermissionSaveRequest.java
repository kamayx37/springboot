package com.example.product.dto;

import com.example.product.entity.SysPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 权限新增 / 修改请求。
 */
@Data
public class PermissionSaveRequest {

    @NotBlank(message = "权限标识不能为空")
    @Size(max = 128, message = "权限标识过长")
    private String code;

    @NotBlank(message = "权限名称不能为空")
    @Size(max = 64, message = "权限名称过长")
    private String name;

    @Pattern(regexp = "^(MENU|BUTTON|API)$", message = "权限类型只能是 MENU / BUTTON / API")
    private String type = "BUTTON";

    private Long parentId = 0L;

    private String path;

    private String icon;

    private Integer sortOrder = 0;

    @Size(max = 255, message = "描述过长")
    private String description;

    public SysPermission toEntity() {
        SysPermission entity = new SysPermission();
        entity.setCode(code);
        entity.setName(name);
        entity.setType(type == null ? "BUTTON" : type);
        entity.setParentId(parentId == null ? 0L : parentId);
        entity.setPath(path);
        entity.setIcon(icon);
        entity.setSortOrder(sortOrder == null ? 0 : sortOrder);
        entity.setDescription(description);
        return entity;
    }
}
