package com.example.product.repository;

import com.example.product.entity.BizProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BizProductRepository extends JpaRepository<BizProduct, Long>, JpaSpecificationExecutor<BizProduct> {

    Optional<BizProduct> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    long countByCategoryId(Long categoryId);

    @Query("select p from BizProduct p order by p.id asc")
    List<BizProduct> findAllForIndex();

    @Query("select p.categoryId, count(p) from BizProduct p group by p.categoryId")
    List<Object[]> countGroupByCategory();
}
