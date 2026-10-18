package com.example.product.dto;

import com.example.product.entity.SysPermission;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限响应（支持树形结构）。
 */
@Data
public class PermissionVO {

    private Long id;
    private String code;
    private String name;
    private String type;
    private Long parentId;
    private String path;
    private String icon;
    private Integer sortOrder;
    private String description;
    private LocalDateTime createdAt;
    private List<PermissionVO> children = new ArrayList<>();

    public static PermissionVO from(SysPermission entity) {
        PermissionVO vo = new PermissionVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setType(entity.getType());
        vo.setParentId(entity.getParentId());
        vo.setPath(entity.getPath());
        vo.setIcon(entity.getIcon());
        vo.setSortOrder(entity.getSortOrder());
        vo.setDescription(entity.getDescription());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    /** 把平铺列表组装成树 */
    public static List<PermissionVO> toTree(List<SysPermission> all) {
        List<PermissionVO> nodes = all.stream().map(PermissionVO::from).toList();
        List<PermissionVO> roots = new ArrayList<>();
        for (PermissionVO node : nodes) {
            if (node.getParentId() == null || node.getParentId() == 0L) {
                roots.add(node);
            } else {
                nodes.stream()
                        .filter(parent -> parent.getId().equals(node.getParentId()))
                        .findFirst()
                        .ifPresentOrElse(parent -> parent.getChildren().add(node), () -> roots.add(node));
            }
        }
        return roots;
    }
}
