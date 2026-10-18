package com.example.product.controller;

import com.example.product.aop.OperationLog;
import com.example.product.common.PageResult;
import com.example.product.common.Result;
import com.example.product.dto.ProductSaveRequest;
import com.example.product.dto.ProductVO;
import com.example.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品管理接口（核心业务模块二）：CRUD + 分页 + 条件筛选。
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAuthority('product:view')")
    public Result<PageResult<ProductVO>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                              @RequestParam(defaultValue = "10") @Min(1) int size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(required = false) String brand,
                                              @RequestParam(required = false) BigDecimal minPrice,
                                              @RequestParam(required = false) BigDecimal maxPrice) {
        return Result.success(productService.page(page, size, keyword, categoryId, status, brand, minPrice, maxPrice));
    }

    @GetMapping("/brands")
    public Result<List<String>> brands() {
        return Result.success(productService.listBrands());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:view')")
    public Result<ProductVO> getById(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:create')")
    @OperationLog(module = "商品管理", operation = "新增商品")
    public Result<ProductVO> create(@Valid @RequestBody ProductSaveRequest request) {
        return Result.success("新增商品成功", productService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:update')")
    @OperationLog(module = "商品管理", operation = "修改商品")
    public Result<ProductVO> update(@PathVariable Long id, @Valid @RequestBody ProductSaveRequest request) {
        return Result.success("修改商品成功", productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    @OperationLog(module = "商品管理", operation = "删除商品")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success("删除商品成功", null);
    }

    @PostMapping("/batch-delete")
    @PreAuthorize("hasAuthority('product:delete')")
    @OperationLog(module = "商品管理", operation = "批量删除商品")
    public Result<Void> batchDelete(@RequestBody Map<String, List<Long>> body) {
        productService.batchDelete(body.get("ids"));
        return Result.success("批量删除成功", null);
    }
}
