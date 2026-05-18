package com.lumenml.service;

import com.lumenml.domain.TaskStatus;
import com.lumenml.repository.TrainingTaskRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrainingFailureHandler {

    private final TrainingTaskRepository trainingTaskRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID id, String err) {
        trainingTaskRepository.findById(id).ifPresent(t -> {
            t.setStatus(TaskStatus.FAILED);
            t.setFinishedAt(Instant.now());
            String msg = err == null ? "Unknown error" : err;
            t.setErrorMessage(msg.length() > 2000 ? msg.substring(0, 2000) : msg);
            trainingTaskRepository.save(t);
        });
    }
}
