package com.example.product.mq;

import com.example.product.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 索引重建任务生产者：把任务编号投递到 RabbitMQ，由消费者在后台执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexTaskProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendRebuildTask(String taskNo) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.INDEX_EXCHANGE,
                RabbitMQConfig.INDEX_ROUTING_KEY,
                taskNo);
        log.info("索引重建任务已投递到 RabbitMQ: queue={}, taskNo={}",
                RabbitMQConfig.INDEX_QUEUE, taskNo);
    }
}
