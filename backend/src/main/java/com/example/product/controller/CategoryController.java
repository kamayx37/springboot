package com.example.product.controller;

import com.example.product.aop.OperationLog;
import com.example.product.common.PageResult;
import com.example.product.common.Result;
import com.example.product.dto.CategorySaveRequest;
import com.example.product.dto.CategoryVO;
import com.example.product.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类管理接口（核心业务模块一）。
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('category:view')")
    public Result<PageResult<CategoryVO>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                               @RequestParam(defaultValue = "10") @Min(1) int size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer status) {
        return Result.success(categoryService.page(page, size, keyword, status));
    }

    /** 下拉选择用：全部分类 */
    @GetMapping("/all")
    public Result<List<CategoryVO>> listAll() {
        return Result.success(categoryService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('category:view')")
    public Result<CategoryVO> getById(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('category:create')")
    @OperationLog(module = "分类管理", operation = "新增分类")
    public Result<CategoryVO> create(@Valid @RequestBody CategorySaveRequest request) {
        return Result.success("新增分类成功", categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('category:update')")
    @OperationLog(module = "分类管理", operation = "修改分类")
    public Result<CategoryVO> update(@PathVariable Long id, @Valid @RequestBody CategorySaveRequest request) {
        return Result.success("修改分类成功", categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('category:delete')")
    @OperationLog(module = "分类管理", operation = "删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success("删除分类成功", null);
    }
}
