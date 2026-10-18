package com.example.product.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 给用户分配角色 / 给角色分配权限的通用请求体。
 */
@Data
public class AssignRequest {

    @NotNull(message = "目标 id 不能为空")
    private Long id;

    /** 角色 id 集合（分配角色）或权限 id 集合（分配权限） */
    private List<Long> ids;

    public List<Long> safeIds() {
        return ids == null ? List.of() : ids;
    }
}
