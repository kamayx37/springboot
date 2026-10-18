package com.example.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增 / 修改角色请求。
 */
@Data
public class RoleSaveRequest {

    @NotBlank(message = "角色标识不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "角色标识需为大写字母/数字/下划线，长度 2-32")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称过长")
    private String name;

    @Size(max = 255, message = "描述过长")
    private String description;

    private Integer status = 1;

    private Integer sortOrder = 0;

    /** 分配的权限 id */
    private List<Long> permissionIds;
}
