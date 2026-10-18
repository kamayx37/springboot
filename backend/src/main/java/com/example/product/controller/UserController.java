package com.example.product.controller;

import com.example.product.aop.OperationLog;
import com.example.product.common.PageResult;
import com.example.product.common.Result;
import com.example.product.dto.AssignRequest;
import com.example.product.dto.UserSaveRequest;
import com.example.product.dto.UserVO;
import com.example.product.security.SecurityUtils;
import com.example.product.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理接口（RBAC：用户分配角色）。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('user:view')")
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                           @RequestParam(defaultValue = "10") @Min(1) int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer status) {
        return Result.success(userService.page(page, size, keyword, status));
    }

    /** 下拉选择用：全部用户 */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('user:view')")
    public Result<List<UserVO>> listAll() {
        return Result.success(userService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:view')")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    @OperationLog(module = "用户管理", operation = "新增用户")
    public Result<UserVO> create(@Valid @RequestBody UserSaveRequest request) {
        return Result.success("新增用户成功", userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    @OperationLog(module = "用户管理", operation = "修改用户")
    public Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserSaveRequest request) {
        return Result.success("修改用户成功", userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    @OperationLog(module = "用户管理", operation = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success("删除用户成功", null);
    }

    /** 给用户分配角色 */
    @PostMapping("/assign-roles")
    @PreAuthorize("hasAuthority('user:assign')")
    @OperationLog(module = "用户管理", operation = "分配角色")
    public Result<UserVO> assignRoles(@Valid @RequestBody AssignRequest request) {
        return Result.success("分配角色成功", userService.assignRoles(request));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('user:update')")
    @OperationLog(module = "用户管理", operation = "重置密码", saveParams = false)
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("newPassword"));
        return Result.success("重置密码成功", null);
    }

    /** 修改自己的密码 */
    @PostMapping("/change-password")
    @OperationLog(module = "个人中心", operation = "修改密码", saveParams = false)
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(SecurityUtils.getUserId(), request.getOldPassword(), request.getNewPassword());
        return Result.success("密码修改成功，请重新登录", null);
    }

    @Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
