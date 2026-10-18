package com.example.product.mq;

import com.example.product.config.RabbitMQConfig;
import com.example.product.entity.IndexTask;
import com.example.product.repository.IndexTaskRepository;
import com.example.product.search.ProductIndexService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 索引重建任务消费者：在后台线程执行 Elasticsearch 全量重建，并回写任务状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexTaskConsumer {

    private final IndexTaskRepository indexTaskRepository;
    private final ProductIndexService productIndexService;

    @RabbitListener(queues = RabbitMQConfig.INDEX_QUEUE)
    public void onRebuildTask(String taskNo,
                              Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到索引重建任务: {}", taskNo);
        IndexTask task = indexTaskRepository.findByTaskNo(taskNo).orElse(null);
        if (task == null) {
            log.warn("任务不存在，丢弃消息: {}", taskNo);
            channel.basicAck(deliveryTag, false);
            return;
        }

        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        task.setMessage("正在后台重建索引...");
        indexTaskRepository.save(task);

        try {
            ProductIndexService.RebuildResult result = productIndexService.rebuildAll();
            task.setTotalCount(result.getTotal());
            task.setSuccessCount(result.getSuccess());
            task.setFailCount(result.getFailed());
            task.setDurationMs(result.getDurationMs());
            task.setStatus(result.getFailed() > 0 ? "SUCCESS" : "SUCCESS");
            task.setMessage("重建完成：扫描 %d 条，成功 %d 条，失败 %d 条，索引文档数 %d"
                    .formatted(result.getTotal(), result.getSuccess(), result.getFailed(), result.getDocumentCount()));
            task.setFinishedAt(LocalDateTime.now());
            indexTaskRepository.save(task);
            log.info("索引重建任务完成: {} -> {}", taskNo, task.getMessage());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("索引重建任务失败: {}", taskNo, e);
            task.setStatus("FAILED");
            task.setMessage("重建失败: " + e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            indexTaskRepository.save(task);
            // 业务失败不再重新入队，避免无限重试
            channel.basicAck(deliveryTag, false);
        }
    }
}
