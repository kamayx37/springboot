package com.example.product.controller;

import com.example.product.aop.OperationLog;
import com.example.product.common.PageResult;
import com.example.product.common.Result;
import com.example.product.dto.AssignRequest;
import com.example.product.dto.RoleSaveRequest;
import com.example.product.dto.RoleVO;
import com.example.product.service.RoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口（RBAC：角色分配权限）。
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Validated
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('role:view')")
    public Result<PageResult<RoleVO>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                           @RequestParam(defaultValue = "10") @Min(1) int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer status) {
        return Result.success(roleService.page(page, size, keyword, status));
    }

    /** 下拉选择用：全部角色（含权限 id） */
    @GetMapping("/all")
    public Result<List<RoleVO>> listAll() {
        return Result.success(roleService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:view')")
    public Result<RoleVO> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    @OperationLog(module = "角色管理", operation = "新增角色")
    public Result<RoleVO> create(@Valid @RequestBody RoleSaveRequest request) {
        return Result.success("新增角色成功", roleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    @OperationLog(module = "角色管理", operation = "修改角色")
    public Result<RoleVO> update(@PathVariable Long id, @Valid @RequestBody RoleSaveRequest request) {
        return Result.success("修改角色成功", roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    @OperationLog(module = "角色管理", operation = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success("删除角色成功", null);
    }

    /** 给角色分配权限 */
    @PostMapping("/assign-permissions")
    @PreAuthorize("hasAuthority('role:assign')")
    @OperationLog(module = "角色管理", operation = "分配权限")
    public Result<RoleVO> assignPermissions(@Valid @RequestBody AssignRequest request) {
        return Result.success("分配权限成功", roleService.assignPermissions(request));
    }
}
