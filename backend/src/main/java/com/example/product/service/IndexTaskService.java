package com.example.product.service;

import com.example.product.common.BusinessException;
import com.example.product.common.PageResult;
import com.example.product.common.ResultCode;
import com.example.product.config.RabbitMQConfig;
import com.example.product.entity.IndexTask;
import com.example.product.mq.IndexTaskProducer;
import com.example.product.repository.IndexTaskRepository;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 索引重建任务：提交（入库 + 投递 MQ）与查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexTaskService {

    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final IndexTaskRepository indexTaskRepository;
    private final IndexTaskProducer indexTaskProducer;
    private final ProductIndexService productIndexService;

    /**
     * 提交全量重建任务：先落库为 PENDING，再投递 RabbitMQ 异步执行。
     */
    @Transactional
    public IndexTask submit(String createdBy) {
        IndexTask task = new IndexTask();
        task.setTaskNo(generateTaskNo());
        task.setType("FULL_REBUILD");
        task.setStatus("PENDING");
        task.setIndexName(productIndexService.indexName());
        task.setCreatedBy(createdBy);
        task.setMessage("任务已提交，等待后台消费");
        IndexTask saved = indexTaskRepository.save(task);

        // 投递到 RabbitMQ，由 IndexTaskConsumer 后台消费
        indexTaskProducer.sendRebuildTask(saved.getTaskNo());
        log.info("用户 {} 提交索引全量重建任务: {}", createdBy, saved.getTaskNo());
        return saved;
    }

    @Transactional(readOnly = true)
    public PageResult<IndexTask> page(int page, int size, String status) {
        Specification<IndexTask> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<IndexTask> result = indexTaskRepository.findAll(spec,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.of(result);
    }

    @Transactional(readOnly = true)
    public List<IndexTask> recent() {
        return indexTaskRepository.findTop20ByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public IndexTask getByTaskNo(String taskNo) {
        return indexTaskRepository.findByTaskNo(taskNo)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "任务不存在: " + taskNo));
    }

    /** 索引进度概览（含 ES 中的文档数） */
    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        return Map.of(
                "indexStatus", productIndexService.indexStatus(),
                "queue", RabbitMQConfig.INDEX_QUEUE,
                "exchange", RabbitMQConfig.INDEX_EXCHANGE,
                "routingKey", RabbitMQConfig.INDEX_ROUTING_KEY,
                "recentTasks", recent());
    }

    private String generateTaskNo() {
        return "IDX" + LocalDateTime.now().format(NO_FORMAT)
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
