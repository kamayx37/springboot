package com.example.product.repository;

import com.example.product.entity.BizCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface BizCategoryRepository extends JpaRepository<BizCategory, Long>, JpaSpecificationExecutor<BizCategory> {

    Optional<BizCategory> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<BizCategory> findByParentId(Long parentId);

    List<BizCategory> findAllByOrderBySortOrderAscIdAsc();

    long countByParentId(Long parentId);
}
