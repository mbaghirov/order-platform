package com.mansur.orderplatform.notificationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    /**
     * Retry a failed message 3 times, 1 second apart. If it still fails, publish it
     * to "<topic>.DLT" so it can be inspected later instead of blocking the partition.
     * Spring Boot plugs this handler into the listener container automatically.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }

    @Bean
    public NewTopic orderEventsTopic(@Value("${app.kafka.topic.order-events}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    /** Same partition count as the main topic: the recoverer keeps the original partition. */
    @Bean
    public NewTopic orderEventsDeadLetterTopic(@Value("${app.kafka.topic.order-events}") String name) {
        return TopicBuilder.name(name + ".DLT").partitions(3).replicas(1).build();
    }
}
