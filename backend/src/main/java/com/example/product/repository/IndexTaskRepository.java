package com.example.product.repository;

import com.example.product.entity.IndexTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface IndexTaskRepository extends JpaRepository<IndexTask, Long>, JpaSpecificationExecutor<IndexTask> {

    Optional<IndexTask> findByTaskNo(String taskNo);

    List<IndexTask> findTop20ByOrderByIdDesc();
}
