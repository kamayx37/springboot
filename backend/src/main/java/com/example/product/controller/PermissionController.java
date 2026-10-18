package com.example.product.controller;

import com.example.product.aop.OperationLog;
import com.example.product.common.PageResult;
import com.example.product.common.Result;
import com.example.product.dto.PermissionSaveRequest;
import com.example.product.dto.PermissionVO;
import com.example.product.service.PermissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理接口。
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Validated
public class PermissionController {

    private final PermissionService permissionService;

    /** 树形权限（角色分配权限时使用） */
    @GetMapping("/tree")
    public Result<List<PermissionVO>> tree() {
        return Result.success(permissionService.tree());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('permission:view')")
    public Result<PageResult<PermissionVO>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                 @RequestParam(defaultValue = "10") @Min(1) int size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String type) {
        return Result.success(permissionService.page(page, size, keyword, type));
    }

    @GetMapping("/all")
    public Result<List<PermissionVO>> listAll() {
        return Result.success(permissionService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:view')")
    public Result<PermissionVO> getById(@PathVariable Long id) {
        return Result.success(permissionService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('permission:create')")
    @OperationLog(module = "权限管理", operation = "新增权限")
    public Result<PermissionVO> create(@Valid @RequestBody PermissionSaveRequest request) {
        return Result.success("新增权限成功", permissionService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:update')")
    @OperationLog(module = "权限管理", operation = "修改权限")
    public Result<PermissionVO> update(@PathVariable Long id, @Valid @RequestBody PermissionSaveRequest request) {
        return Result.success("修改权限成功", permissionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:delete')")
    @OperationLog(module = "权限管理", operation = "删除权限")
    public Result<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return Result.success("删除权限成功", null);
    }
}
