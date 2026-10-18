package com.example.product.dto;

import com.example.product.entity.BizCategory;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类响应。
 */
@Data
public class CategoryVO {

    private Long id;
    private String name;
    private String code;
    private Long parentId;
    private String parentName;
    private Integer sortOrder;
    private Integer status;
    private String description;
    private LocalDateTime createdAt;
    private Long productCount;

    public static CategoryVO from(BizCategory entity) {
        CategoryVO vo = new CategoryVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setCode(entity.getCode());
        vo.setParentId(entity.getParentId());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setDescription(entity.getDescription());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
