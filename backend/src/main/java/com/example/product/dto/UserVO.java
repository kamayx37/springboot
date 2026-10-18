package com.example.product.dto;

import com.example.product.entity.SysUser;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户列表 / 详情响应。
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private List<RoleBrief> roles;

    @Data
    public static class RoleBrief {
        private Long id;
        private String code;
        private String name;
    }

    public static UserVO from(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setRemark(user.getRemark());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setRoles(user.getRoles().stream().map(r -> {
            RoleBrief brief = new RoleBrief();
            brief.setId(r.getId());
            brief.setCode(r.getCode());
            brief.setName(r.getName());
            return brief;
        }).toList());
        return vo;
    }
}
