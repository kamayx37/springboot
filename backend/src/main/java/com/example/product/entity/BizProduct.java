package com.example.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品（核心业务实体，同时是 Elasticsearch 索引的数据来源）。
 */
@Getter
@Setter
@Entity
@Table(name = "biz_product", indexes = {
        @Index(name = "idx_biz_product_code", columnList = "code", unique = true),
        @Index(name = "idx_biz_product_category", columnList = "category_id"),
        @Index(name = "idx_biz_product_name", columnList = "name")
})
public class BizProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 商品名称 */
    @Column(nullable = false, length = 128)
    private String name;

    /** 商品编码 / SKU */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /** 所属分类 */
    @Column(name = "category_id")
    private Long categoryId;

    /** 品牌 */
    @Column(length = 64)
    private String brand;

    /** 售价 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    /** 库存 */
    @Column(nullable = false)
    private Integer stock = 0;

    /** 计量单位 */
    @Column(length = 16)
    private String unit = "件";

    /** 1=上架 0=下架 */
    @Column(nullable = false)
    private Integer status = 1;

    /** 销量 */
    @Column(nullable = false)
    private Integer sales = 0;

    /** 商品描述（参与全文检索） */
    @Column(length = 2000)
    private String description;

    /** 封面图 */
    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = 1;
        }
        if (this.stock == null) {
            this.stock = 0;
        }
        if (this.sales == null) {
            this.sales = 0;
        }
        if (this.price == null) {
            this.price = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
