package com.example.product.service;

import com.example.product.common.PageResult;
import com.example.product.entity.SysOperationLog;
import com.example.product.repository.SysOperationLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志查询。
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final SysOperationLogRepository logRepository;

    @Transactional(readOnly = true)
    public PageResult<SysOperationLog> page(int page, int size, String username, String module,
                                            String operation, Integer success,
                                            LocalDate startDate, LocalDate endDate) {
        Specification<SysOperationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(username)) {
                predicates.add(cb.like(cb.lower(root.get("username")),
                        "%" + username.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(module)) {
                predicates.add(cb.equal(root.get("module"), module));
            }
            if (StringUtils.hasText(operation)) {
                predicates.add(cb.like(cb.lower(root.get("operation")),
                        "%" + operation.trim().toLowerCase() + "%"));
            }
            if (success != null) {
                predicates.add(cb.equal(root.get("success"), success));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        LocalDateTime.of(startDate, LocalTime.MIN)));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"),
                        LocalDateTime.of(endDate, LocalTime.MAX)));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<SysOperationLog> result = logRepository.findAll(spec,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.of(result);
    }

    @Transactional(readOnly = true)
    public List<String> listModules() {
        return logRepository.findAll().stream()
                .map(SysOperationLog::getModule)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional
    public void clearAll() {
        logRepository.deleteAll();
    }
}
