package com.example.product.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 全文检索查询条件（关键词 + 组合筛选 + 排序 + 分页）。
 */
@Data
public class ProductSearchRequest {

    /** 关键词，命中 name / brand / description / code */
    private String keyword;

    private Long categoryId;

    private String brand;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    /** 1=上架 0=下架，null 表示不限 */
    private Integer status;

    private Integer page = 1;

    private Integer size = 10;

    /** 排序字段：_score / price / sales / createdAt */
    private String sortField = "_score";

    /** asc / desc */
    private String sortOrder = "desc";

    public int safePage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int safeSize() {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
