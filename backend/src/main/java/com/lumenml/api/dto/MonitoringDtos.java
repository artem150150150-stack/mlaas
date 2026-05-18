package com.lumenml.api.dto;

import java.util.Map;

public class MonitoringDtos {

    public record QueueInfo(String queue, Long messageCount, Long consumerCount) {}

    public record MonitoringDashboard(QueueInfo trainingQueue, long runningTasks, Map<String, Object> hints) {}
}
