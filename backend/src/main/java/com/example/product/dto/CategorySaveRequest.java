package com.example.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类新增 / 修改请求。
 */
@Data
public class CategorySaveRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称过长")
    private String name;

    @NotBlank(message = "分类编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,32}$", message = "分类编码只能包含字母、数字、-、_，长度 2-32")
    private String code;

    private Long parentId = 0L;

    private Integer sortOrder = 0;

    private Integer status = 1;

    @Size(max = 255, message = "描述过长")
    private String description;
}
