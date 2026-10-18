package com.example.product.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增 / 修改用户请求。
 */
@Data
public class UserSaveRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,32}$", message = "用户名只能包含字母、数字、下划线，长度 3-32")
    private String username;

    /** 新增时必填；修改时留空表示不改密码 */
    @Size(min = 4, max = 64, message = "密码长度需在 4-64 之间")
    private String password;

    @Size(max = 64, message = "昵称过长")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private Integer status = 1;

    @Size(max = 255, message = "备注过长")
    private String remark;

    /** 分配的角色 id */
    private List<Long> roleIds;
}
