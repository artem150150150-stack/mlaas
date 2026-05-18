package com.lumenml.rabbit;



import com.lumenml.config.LumenMlProperties;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.stereotype.Component;



@Slf4j

@Component

@RequiredArgsConstructor

public class TrainingJobProducer {



    private final RabbitTemplate rabbitTemplate;

    private final LumenMlProperties props;



    public void enqueue(UUID taskId) {

        TrainingJobMessage msg = new TrainingJobMessage(taskId);

        rabbitTemplate.convertAndSend(

                props.getRabbit().getTrainingExchange(), props.getRabbit().getTrainingRoutingKey(), msg);

        log.debug("Published training job {}", taskId);

    }

}

