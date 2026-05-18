package com.lumenml;

import org.springframework.boot.SpringApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableRetry
@EnableRabbit
public class LumenMlApplication {

    public static void main(String[] args) {
        SpringApplication.run(LumenMlApplication.class, args);
    }
}
