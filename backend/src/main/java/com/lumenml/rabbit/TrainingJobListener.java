package com.lumenml.rabbit;

import com.lumenml.service.TrainingFailureHandler;
import com.lumenml.service.TrainingJobProcessor;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("worker")
@RequiredArgsConstructor
public class TrainingJobListener {

    private final TrainingJobProcessor processor;
    private final TrainingFailureHandler failureHandler;

    @RabbitListener(queues = TrainingRabbitConfig.QUEUE_TRAINING, ackMode = "AUTO")
    public void onMessage(TrainingJobMessage message) {
        UUID id = message.taskId();
        try {
            processor.handle(id);
        } catch (Exception e) {
            log.error("Training failed for {}", id, e);
            failureHandler.markFailed(id, e.getMessage());
        }
    }
}
