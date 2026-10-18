package com.example.product.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 全文检索结果项（含高亮片段）。
 */
@Data
public class ProductSearchVO implements Serializable {

    private Long id;
    private String name;
    /** 名称高亮（含 &lt;em&gt; 标签） */
    private String nameHighlight;
    private String code;
    private String brand;
    private String brandHighlight;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private Integer stock;
    private Integer status;
    private Integer sales;
    private String description;
    /** 描述高亮片段（含 &lt;em&gt; 标签） */
    private String descriptionHighlight;
    private String coverUrl;
    private Double score;

    /** 所有高亮片段汇总，方便前端统一展示 */
    private List<HighlightFragment> highlights = new ArrayList<>();

    @Data
    public static class HighlightFragment {
        private String field;
        private String fragment;

        public HighlightFragment() {
        }

        public HighlightFragment(String field, String fragment) {
            this.field = field;
            this.fragment = fragment;
        }
    }
}
