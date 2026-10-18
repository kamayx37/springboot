package com.example.product.dto;

import com.example.product.entity.BizProduct;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品响应。
 */
@Data
public class ProductVO {

    private Long id;
    private String name;
    private String code;
    private Long categoryId;
    private String categoryName;
    private String brand;
    private BigDecimal price;
    private Integer stock;
    private String unit;
    private Integer status;
    private Integer sales;
    private String description;
    private String coverUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductVO from(BizProduct entity) {
        return from(entity, null);
    }

    public static ProductVO from(BizProduct entity, String categoryName) {
        ProductVO vo = new ProductVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setCode(entity.getCode());
        vo.setCategoryId(entity.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setBrand(entity.getBrand());
        vo.setPrice(entity.getPrice());
        vo.setStock(entity.getStock());
        vo.setUnit(entity.getUnit());
        vo.setStatus(entity.getStatus());
        vo.setSales(entity.getSales());
        vo.setDescription(entity.getDescription());
        vo.setCoverUrl(entity.getCoverUrl());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
