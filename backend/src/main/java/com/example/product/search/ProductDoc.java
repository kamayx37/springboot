package com.example.product.search;

import com.example.product.entity.BizProduct;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Elasticsearch 索引文档模型。
 */
@Data
public class ProductDoc implements Serializable {

    private Long id;
    private String name;
    private String code;
    private String brand;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private Integer stock;
    private Integer status;
    private Integer sales;
    private String description;
    private String coverUrl;
    /** 格式 yyyy-MM-dd HH:mm:ss */
    private String createdAt;

    public static ProductDoc from(BizProduct entity, String categoryName) {
        ProductDoc doc = new ProductDoc();
        doc.setId(entity.getId());
        doc.setName(entity.getName());
        doc.setCode(entity.getCode());
        doc.setBrand(entity.getBrand());
        doc.setCategoryId(entity.getCategoryId());
        doc.setCategoryName(categoryName);
        doc.setPrice(entity.getPrice());
        doc.setStock(entity.getStock());
        doc.setStatus(entity.getStatus());
        doc.setSales(entity.getSales());
        doc.setDescription(entity.getDescription());
        doc.setCoverUrl(entity.getCoverUrl());
        doc.setCreatedAt(entity.getCreatedAt() == null ? null
                : entity.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return doc;
    }
}
