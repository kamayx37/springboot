package com.example.product.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：索引重建任务使用「直连交换机 + 持久化队列」。
 */
@Configuration
public class RabbitMQConfig {

    /** 索引任务交换机 */
    public static final String INDEX_EXCHANGE = "es.index.exchange";
    /** 索引重建队列（持久化） */
    public static final String INDEX_QUEUE = "es.index.rebuild.queue";
    /** 路由键 */
    public static final String INDEX_ROUTING_KEY = "es.index.rebuild";

    @Bean
    public DirectExchange indexExchange() {
        return new DirectExchange(INDEX_EXCHANGE, true, false);
    }

    @Bean
    public Queue indexQueue() {
        return new Queue(INDEX_QUEUE, true);
    }

    @Bean
    public Binding indexBinding(Queue indexQueue, DirectExchange indexExchange) {
        return BindingBuilder.bind(indexQueue).to(indexExchange).with(INDEX_ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new SimpleMessageConverter());
        return template;
    }
}
