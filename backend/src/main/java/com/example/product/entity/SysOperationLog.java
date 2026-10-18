package com.example.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 操作日志（由 AOP 切面写入）。
 */
@Getter
@Setter
@Entity
@Table(name = "sys_operation_log", indexes = {
        @Index(name = "idx_oplog_created_at", columnList = "created_at"),
        @Index(name = "idx_oplog_username", columnList = "username")
})
public class SysOperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作人 */
    @Column(length = 64)
    private String username;

    /** 业务模块，如「商品管理」 */
    @Column(length = 64)
    private String module;

    /** 操作描述，如「新增商品」 */
    @Column(length = 128)
    private String operation;

    /** 请求方式 GET/POST/PUT/DELETE */
    @Column(name = "http_method", length = 16)
    private String httpMethod;

    /** 请求路径 */
    @Column(name = "request_uri", length = 255)
    private String requestUri;

    /** 请求参数（截断保存） */
    @Column(length = 2000)
    private String params;

    /** 客户端 IP */
    @Column(length = 64)
    private String ip;

    /** 耗时（毫秒） */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** 1=成功 0=失败 */
    @Column(nullable = false)
    private Integer success = 1;

    /** 失败时的异常信息 */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
