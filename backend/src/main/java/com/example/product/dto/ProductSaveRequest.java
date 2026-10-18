package com.example.product.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品新增 / 修改请求。
 */
@Data
public class ProductSaveRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 128, message = "商品名称过长")
    private String name;

    @NotBlank(message = "商品编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,64}$", message = "商品编码只能包含字母、数字、-、_，长度 2-64")
    private String code;

    @NotNull(message = "请选择所属分类")
    private Long categoryId;

    @Size(max = 64, message = "品牌名称过长")
    private String brand;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.00", message = "售价不能为负数")
    @Digits(integer = 10, fraction = 2, message = "售价格式不正确")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Size(max = 16, message = "单位过长")
    private String unit = "件";

    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    private Integer status = 1;

    @Min(value = 0, message = "销量不能为负数")
    private Integer sales = 0;

    @Size(max = 2000, message = "商品描述过长")
    private String description;

    @Size(max = 255, message = "封面地址过长")
    private String coverUrl;
}
