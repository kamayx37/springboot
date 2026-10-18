package com.example.product.controller;

import com.example.product.common.PageResult;
import com.example.product.common.Result;
import com.example.product.dto.ProductSearchRequest;
import com.example.product.dto.ProductSearchVO;
import com.example.product.search.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 全文检索接口（Elasticsearch + 高亮）。
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchService productSearchService;

    /** 关键词搜索 + 组合条件筛选 + 高亮 */
    @PostMapping("/products")
    @PreAuthorize("hasAuthority('search:view')")
    public Result<PageResult<ProductSearchVO>> search(@RequestBody ProductSearchRequest request) {
        return Result.success(productSearchService.search(request));
    }

    /** 也支持 GET，方便直接用浏览器/curl 验证 */
    @GetMapping("/products")
    @PreAuthorize("hasAuthority('search:view')")
    public Result<PageResult<ProductSearchVO>> searchByGet(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword(keyword);
        request.setCategoryId(categoryId);
        request.setBrand(brand);
        request.setStatus(status);
        request.setPage(page);
        request.setSize(size);
        return Result.success(productSearchService.search(request));
    }

    /** 索引状态与筛选元数据 */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.success(productSearchService.filters());
    }
}
