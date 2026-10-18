package com.example.product.controller;

import com.example.product.aop.OperationLog;
import com.example.product.common.PageResult;
import com.example.product.common.Result;
import com.example.product.entity.IndexTask;
import com.example.product.security.SecurityUtils;
import com.example.product.service.IndexTaskService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Elasticsearch 索引重建任务接口（提交后由 RabbitMQ 后台消费）。
 */
@RestController
@RequestMapping("/api/index-tasks")
@RequiredArgsConstructor
@Validated
public class IndexTaskController {

    private final IndexTaskService indexTaskService;

    /** 提交全量重建索引任务 */
    @PostMapping("/rebuild")
    @PreAuthorize("hasAuthority('search:rebuild')")
    @OperationLog(module = "全文检索", operation = "提交索引全量重建")
    public Result<IndexTask> rebuild() {
        IndexTask task = indexTaskService.submit(SecurityUtils.getUsername());
        return Result.success("索引重建任务已提交，正在后台执行", task);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('search:view')")
    public Result<PageResult<IndexTask>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                              @RequestParam(defaultValue = "10") @Min(1) int size,
                                              @RequestParam(required = false) String status) {
        return Result.success(indexTaskService.page(page, size, status));
    }

    /** 任务与索引概览 */
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('search:view')")
    public Result<Map<String, Object>> overview() {
        return Result.success(indexTaskService.overview());
    }

    @GetMapping("/{taskNo}")
    @PreAuthorize("hasAuthority('search:view')")
    public Result<IndexTask> getByTaskNo(@PathVariable String taskNo) {
        return Result.success(indexTaskService.getByTaskNo(taskNo));
    }
}
