package com.example.product.controller;

import com.example.product.aop.OperationLog;
import com.example.product.common.PageResult;
import com.example.product.common.Result;
import com.example.product.entity.SysOperationLog;
import com.example.product.service.OperationLogService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 操作日志查询接口（数据由 AOP 切面自动记录）。
 */
@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
@Validated
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('log:view')")
    public Result<PageResult<SysOperationLog>> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) Integer success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(operationLogService.page(page, size, username, module, operation,
                success, startDate, endDate));
    }

    @GetMapping("/modules")
    @PreAuthorize("hasAuthority('log:view')")
    public Result<List<String>> modules() {
        return Result.success(operationLogService.listModules());
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('log:clear')")
    @OperationLog(module = "操作日志", operation = "清空操作日志")
    public Result<Void> clear() {
        operationLogService.clearAll();
        return Result.success("操作日志已清空", null);
    }
}
