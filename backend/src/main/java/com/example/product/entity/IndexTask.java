package com.example.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Elasticsearch 全量索引重建任务（由 RabbitMQ 异步消费执行）。
 */
@Getter
@Setter
@Entity
@Table(name = "sys_index_task", indexes = {
        @Index(name = "idx_index_task_created", columnList = "created_at")
})
public class IndexTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务编号，同时作为 MQ 消息体 */
    @Column(name = "task_no", nullable = false, unique = true, length = 64)
    private String taskNo;

    /** 任务类型：FULL_REBUILD */
    @Column(nullable = false, length = 32)
    private String type = "FULL_REBUILD";

    /** PENDING / RUNNING / SUCCESS / FAILED */
    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    /** 目标索引 */
    @Column(name = "index_name", length = 64)
    private String indexName;

    /** 扫描总数 */
    @Column(name = "total_count")
    private Long totalCount = 0L;

    /** 成功写入数 */
    @Column(name = "success_count")
    private Long successCount = 0L;

    /** 失败数 */
    @Column(name = "fail_count")
    private Long failCount = 0L;

    /** 耗时（毫秒） */
    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(length = 1000)
    private String message;

    /** 提交人 */
    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
