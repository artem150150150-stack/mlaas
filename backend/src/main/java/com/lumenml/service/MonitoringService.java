package com.lumenml.service;

import com.lumenml.api.dto.DriftSimulateRequest;
import com.lumenml.api.dto.MonitoringDtos.MonitoringDashboard;
import com.lumenml.api.dto.MonitoringDtos.QueueInfo;
import com.lumenml.domain.MetricSnapshot;
import com.lumenml.domain.TaskStatus;
import com.lumenml.domain.TrainingTask;
import com.lumenml.exception.NotFoundException;
import com.lumenml.rabbit.TrainingRabbitConfig;
import com.lumenml.repository.MetricSnapshotRepository;
import com.lumenml.repository.TrainingTaskRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final RabbitAdmin rabbitAdmin;
    private final TrainingTaskRepository trainingTaskRepository;
    private final MetricSnapshotRepository metricSnapshotRepository;

    @Transactional
    public void simulateDrift(DriftSimulateRequest request) {
        TrainingTask task = trainingTaskRepository
                .findById(request.trainingTaskId())
                .orElseThrow(() -> new NotFoundException("Task not found"));
        metricSnapshotRepository.save(MetricSnapshot.builder()
                .trainingTask(task)
                .metricName("drift_simulation")
                .metricValue(request.driftScore())
                .driftScore(request.driftScore())
                .simulated(request.simulated())
                .build());
    }

    @Transactional(readOnly = true)
    public MonitoringDashboard dashboard() {
        QueueInfo q = queue(TrainingRabbitConfig.QUEUE_TRAINING);
        long running = trainingTaskRepository.countByStatus(TaskStatus.RUNNING);
        Map<String, Object> hints = new HashMap<>();
        hints.put("metricSnapshotsTotal", metricSnapshotRepository.count());
        return new MonitoringDashboard(q, running, hints);
    }

    private QueueInfo queue(String name) {
        Properties props = rabbitAdmin.getQueueProperties(name);
        if (props == null) {
            return new QueueInfo(name, null, null);
        }
        Object messages = props.get("QUEUE_MESSAGE_COUNT");
        Object consumers = props.get("QUEUE_CONSUMER_COUNT");
        Long msg = messages instanceof Number ? ((Number) messages).longValue() : null;
        Long cons = consumers instanceof Number ? ((Number) consumers).longValue() : null;
        return new QueueInfo(name, msg, cons);
    }
}
