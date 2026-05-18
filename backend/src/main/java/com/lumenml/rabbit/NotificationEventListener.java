package com.lumenml.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("worker")
public class NotificationEventListener {

    @RabbitListener(queues = TrainingRabbitConfig.QUEUE_NOTIFICATIONS, ackMode = "AUTO")
    public void onNotification(NotificationEvent event) {
        log.info("[notification] type={} task={} msg={}", event.type(), event.taskId(), event.message());
    }
}
