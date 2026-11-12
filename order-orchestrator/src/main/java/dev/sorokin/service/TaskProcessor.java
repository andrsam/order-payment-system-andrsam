package dev.sorokin.service;

import dev.sorokin.config.properties.TaskProcessorProperties;
import dev.sorokin.domain.Task;
import dev.sorokin.enums.TaskStatus;
import dev.sorokin.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskProcessor {
    private final TaskRepository taskRepository;
    private final ExecutorService taskDispatcherThreadPool;
    private final OrderProcessor orderProcessor;
    private final TaskProcessorProperties properties;

    @Transactional
    public void process(Task task) {
        CompletableFuture
                .supplyAsync(() -> orderProcessor.process(task), taskDispatcherThreadPool)
                .thenAccept(status -> handleTaskExecuted(task, status))
                .exceptionally(e -> handleException(task, e));
    }

    private void handleTaskExecuted(Task task, TaskStatus status) {
        log.info("Task id = [{}] was executed", task.getId());

        switch (status) {
            case SUCCEEDED -> handleTaskSuccessfully(task);
            case FAILED_NON_RETRYABLE -> handleTaskFailed(task);
            case FAILED_RETRYABLE -> scheduleTaskRetry(task);
        }
    }

    private void handleTaskSuccessfully(Task task) {
        taskRepository.save(task.toBuilder()
                .status(TaskStatus.DONE)
                .nextAttemptAt(null)
                .build());
        log.info("Task id = [{}]: successfully finished processing", task.getId());
    }

    private void handleTaskFailed(Task task) {
        taskRepository.save(task.toBuilder()
                        .status(TaskStatus.FAILED_NON_RETRYABLE)
                .nextAttemptAt(null)
                .build());
        log.info("Task  id = [{}]: failed, status = [{}] ", task.getId(), task.getStatus());
    }

    private void scheduleTaskRetry(Task task) {
        log.info("Scheduling task with id = [{}] retrying", task.getId());
        if (task.getAttempts() >= properties.getMaxAttempts()) {
            log.error("Maximum number of attempts for task with id = [{}] reached", task.getId());
            taskRepository.save(task.toBuilder()
                    .status(TaskStatus.FAILED_NON_RETRYABLE)
                    .nextAttemptAt(null)
                    .build());
        }

        val nextAttemptAt = LocalDateTime.now().plus(Duration.ofSeconds(5));
        taskRepository.save(task.toBuilder()
                .status(TaskStatus.FAILED_RETRYABLE)
                .nextAttemptAt(nextAttemptAt)
                .build());
    }

    private Void handleException(Task task, Throwable e) {
        log.error("Error executing task with id = [{}] ", task.getId(), e);
        scheduleTaskRetry(task);
        return null;
    }
}
