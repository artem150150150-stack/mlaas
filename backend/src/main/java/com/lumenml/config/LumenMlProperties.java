package com.lumenml.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "lumenml")
public class LumenMlProperties {

    private final Jwt jwt = new Jwt();
    private final Storage storage = new Storage();
    private final Rabbit rabbit = new Rabbit();
    private final RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "change-me";
        private long accessExpirationMs = 900_000;
        private long refreshExpirationMs = 604_800_000L;
    }

    @Getter
    @Setter
    public static class Storage {
        private String datasetsDir = "./data/datasets";
    }

    @Getter
    @Setter
    public static class Rabbit {
        private String trainingExchange = "lumenml.training";
        private String trainingRoutingKey = "training.request";
        private String notificationExchange = "lumenml.notifications";
        private String metricsExchange = "lumenml.metrics";
    }

    @Getter
    @Setter
    public static class RateLimit {
        private long capacity = 120;
        private long refillPerMinute = 120;
    }
}
