package com.example.product.service;

import com.example.product.common.BusinessException;
import com.example.product.common.PageResult;
import com.example.product.common.ResultCode;
import com.example.product.dto.CategorySaveRequest;
import com.example.product.dto.CategoryVO;
import com.example.product.entity.BizCategory;
import com.example.product.repository.BizCategoryRepository;
import com.example.product.repository.BizProductRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品分类管理（核心业务模块一）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final BizCategoryRepository categoryRepository;
    private final BizProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResult<CategoryVO> page(int page, int size, String keyword, Integer status) {
        Specification<BizCategory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("code")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<BizCategory> result = categoryRepository.findAll(spec,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.ASC, "sortOrder", "id")));
        return PageResult.of(result, this::toVO);
    }

    @Transactional(readOnly = true)
    public List<CategoryVO> listAll() {
        Map<Long, BizCategory> byId = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(BizCategory::getId, Function.identity()));
        Map<Long, Long> productCounts = productRepository.countGroupByCategory().stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).longValue(),
                        (a, b) -> a));
        return categoryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(c -> {
                    CategoryVO vo = CategoryVO.from(c);
                    BizCategory parent = byId.get(c.getParentId());
                    vo.setParentName(parent == null ? null : parent.getName());
                    vo.setProductCount(productCounts.getOrDefault(c.getId(), 0L));
                    return vo;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryVO getById(Long id) {
        return toVO(findCategory(id));
    }

    @Transactional
    public CategoryVO create(CategorySaveRequest request) {
        if (categoryRepository.existsByCode(request.getCode())) {
            throw new BusinessException(ResultCode.CONFLICT, "分类编码已存在: " + request.getCode());
        }
        checkParent(request.getParentId(), null);
        BizCategory entity = new BizCategory();
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setDescription(request.getDescription());
        BizCategory saved = categoryRepository.save(entity);
        log.info("新增分类: {} ({})", saved.getName(), saved.getCode());
        return toVO(saved);
    }

    @Transactional
    public CategoryVO update(Long id, CategorySaveRequest request) {
        BizCategory entity = findCategory(id);
        if (categoryRepository.existsByCodeAndIdNot(request.getCode(), id)) {
            throw new BusinessException(ResultCode.CONFLICT, "分类编码已被占用: " + request.getCode());
        }
        Long parentId = request.getParentId() == null ? 0L : request.getParentId();
        if (parentId.equals(id)) {
            throw new BusinessException("上级分类不能是自己");
        }
        checkParent(parentId, id);
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setParentId(parentId);
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setDescription(request.getDescription());
        BizCategory saved = categoryRepository.save(entity);
        log.info("修改分类: {}", saved.getCode());
        return toVO(saved);
    }

    @Transactional
    public void delete(Long id) {
        BizCategory entity = findCategory(id);
        if (categoryRepository.countByParentId(id) > 0) {
            throw new BusinessException("该分类下存在子分类，请先删除子分类");
        }
        if (productRepository.countByCategoryId(id) > 0) {
            throw new BusinessException("该分类下存在商品，无法删除");
        }
        categoryRepository.delete(entity);
        log.info("删除分类: {}", entity.getCode());
    }

    private void checkParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        findCategory(parentId);
        if (selfId != null && parentId.equals(selfId)) {
            throw new BusinessException("上级分类不能是自己");
        }
    }

    private CategoryVO toVO(BizCategory entity) {
        CategoryVO vo = CategoryVO.from(entity);
        if (entity.getParentId() != null && entity.getParentId() != 0L) {
            categoryRepository.findById(entity.getParentId())
                    .ifPresent(parent -> vo.setParentName(parent.getName()));
        }
        vo.setProductCount(productRepository.countByCategoryId(entity.getId()));
        return vo;
    }

    private BizCategory findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "分类不存在: " + id));
    }
}
