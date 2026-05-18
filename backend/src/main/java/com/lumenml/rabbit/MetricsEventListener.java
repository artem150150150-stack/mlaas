package com.lumenml.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("worker")
public class MetricsEventListener {

    @RabbitListener(queues = TrainingRabbitConfig.QUEUE_METRICS, ackMode = "AUTO")
    public void onMetrics(MetricsEvent event) {
        log.info("[metrics] task={} {}={}", event.taskId(), event.metricName(), event.value());
    }
}
