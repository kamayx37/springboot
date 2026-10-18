package com.example.product.repository;

import com.example.product.entity.SysOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SysOperationLogRepository extends JpaRepository<SysOperationLog, Long>,
        JpaSpecificationExecutor<SysOperationLog> {
}
