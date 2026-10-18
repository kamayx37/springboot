package com.example.product.service;

import com.example.product.common.BusinessException;
import com.example.product.common.PageResult;
import com.example.product.common.ResultCode;
import com.example.product.dto.ProductSaveRequest;
import com.example.product.dto.ProductVO;
import com.example.product.entity.BizCategory;
import com.example.product.entity.BizProduct;
import com.example.product.repository.BizCategoryRepository;
import com.example.product.repository.BizProductRepository;
import com.example.product.search.ProductIndexService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品管理（核心业务模块二）：CRUD + 分页 + 条件筛选。
 * <p>写操作会同步更新 Elasticsearch 单条文档，保证搜索结果与数据库一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final BizProductRepository productRepository;
    private final BizCategoryRepository categoryRepository;
    private final ProductIndexService productIndexService;

    @Transactional(readOnly = true)
    public PageResult<ProductVO> page(int page, int size, String keyword, Long categoryId,
                                      Integer status, String brand,
                                      BigDecimal minPrice, BigDecimal maxPrice) {
        Specification<BizProduct> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("brand")), like)));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(brand)) {
                predicates.add(cb.equal(root.get("brand"), brand));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<BizProduct> result = productRepository.findAll(spec,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id")));
        Map<Long, String> categoryNames = categoryNameMap();
        return PageResult.of(result, p -> ProductVO.from(p, categoryNames.get(p.getCategoryId())));
    }

    @Transactional(readOnly = true)
    public ProductVO getById(Long id) {
        BizProduct entity = findProduct(id);
        return ProductVO.from(entity, categoryName(entity.getCategoryId()));
    }

    @Transactional(readOnly = true)
    public List<String> listBrands() {
        return productRepository.findAll().stream()
                .map(BizProduct::getBrand)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional
    public ProductVO create(ProductSaveRequest request) {
        if (productRepository.existsByCode(request.getCode())) {
            throw new BusinessException(ResultCode.CONFLICT, "商品编码已存在: " + request.getCode());
        }
        BizCategory category = findCategory(request.getCategoryId());
        BizProduct entity = new BizProduct();
        applyFields(entity, request);
        BizProduct saved = productRepository.save(entity);
        log.info("新增商品: {} ({})", saved.getName(), saved.getCode());

        // 同步写入 ES 索引
        productIndexService.indexOne(saved, category.getName());
        return ProductVO.from(saved, category.getName());
    }

    @Transactional
    public ProductVO update(Long id, ProductSaveRequest request) {
        BizProduct entity = findProduct(id);
        if (productRepository.existsByCodeAndIdNot(request.getCode(), id)) {
            throw new BusinessException(ResultCode.CONFLICT, "商品编码已被占用: " + request.getCode());
        }
        BizCategory category = findCategory(request.getCategoryId());
        applyFields(entity, request);
        BizProduct saved = productRepository.save(entity);
        log.info("修改商品: {}", saved.getCode());

        productIndexService.indexOne(saved, category.getName());
        return ProductVO.from(saved, category.getName());
    }

    @Transactional
    public void delete(Long id) {
        BizProduct entity = findProduct(id);
        productRepository.delete(entity);
        productIndexService.deleteOne(id);
        log.info("删除商品: {}", entity.getCode());
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<BizProduct> products = productRepository.findAllById(ids);
        productRepository.deleteAll(products);
        products.forEach(p -> productIndexService.deleteOne(p.getId()));
        log.info("批量删除商品数量: {}", products.size());
    }

    private void applyFields(BizProduct entity, ProductSaveRequest request) {
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setCategoryId(request.getCategoryId());
        entity.setBrand(request.getBrand());
        entity.setPrice(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        entity.setStock(request.getStock() == null ? 0 : request.getStock());
        entity.setUnit(StringUtils.hasText(request.getUnit()) ? request.getUnit() : "件");
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setSales(request.getSales() == null ? 0 : request.getSales());
        entity.setDescription(request.getDescription());
        entity.setCoverUrl(request.getCoverUrl());
    }

    private Map<Long, String> categoryNameMap() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(BizCategory::getId, BizCategory::getName, (a, b) -> a));
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId).map(BizCategory::getName).orElse(null);
    }

    private BizProduct findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "商品不存在: " + id));
    }

    private BizCategory findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "分类不存在: " + id));
    }

    /** 供索引重建使用：把实体转成检索文档所需的数据 */
    @Transactional(readOnly = true)
    public List<ProductVO> listAllForIndex() {
        Map<Long, String> categoryNames = categoryNameMap();
        return productRepository.findAllForIndex().stream()
                .map(p -> ProductVO.from(p, categoryNames.get(p.getCategoryId())))
                .collect(Collectors.toList());
    }

    /** 忽略未使用的告警 */
    @SuppressWarnings("unused")
    private Function<BizProduct, ProductVO> mapper(Map<Long, String> names) {
        return p -> ProductVO.from(p, names.get(p.getCategoryId()));
    }
}
