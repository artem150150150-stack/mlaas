package com.lumenml.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumenml.config.LumenMlProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrainingRabbitConfig {

    public static final String QUEUE_TRAINING = "training.jobs";
    public static final String QUEUE_TRAINING_DLQ = "training.jobs.dlq";
    public static final String QUEUE_NOTIFICATIONS = "notification.events";
    public static final String QUEUE_METRICS = "metrics.processing";

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        return t;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory cf) {
        return new RabbitAdmin(cf);
    }

    @Bean
    public Declarables trainingTopology(LumenMlProperties props) {
        DirectExchange trainingEx = new DirectExchange(props.getRabbit().getTrainingExchange(), true, false);
        Queue dlq = QueueBuilder.durable(QUEUE_TRAINING_DLQ).build();
        Queue main = QueueBuilder.durable(QUEUE_TRAINING)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_TRAINING_DLQ)
                .build();
        DirectExchange notifEx = new DirectExchange(props.getRabbit().getNotificationExchange(), true, false);
        Queue notifQ = QueueBuilder.durable(QUEUE_NOTIFICATIONS).build();
        DirectExchange metricsEx = new DirectExchange(props.getRabbit().getMetricsExchange(), true, false);
        Queue metricsQ = QueueBuilder.durable(QUEUE_METRICS).build();

        return new Declarables(
                trainingEx,
                main,
                dlq,
                BindingBuilder.bind(main).to(trainingEx).with(props.getRabbit().getTrainingRoutingKey()),
                notifEx,
                notifQ,
                BindingBuilder.bind(notifQ).to(notifEx).with("notification.created"),
                metricsEx,
                metricsQ,
                BindingBuilder.bind(metricsQ).to(metricsEx).with("metrics.process"));
    }
}
