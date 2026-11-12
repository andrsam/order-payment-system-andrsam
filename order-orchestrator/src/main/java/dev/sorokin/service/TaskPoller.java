package dev.sorokin.service;

import dev.sorokin.config.properties.TaskPollerProperties;
import dev.sorokin.domain.Task;
import dev.sorokin.enums.TaskStatus;
import dev.sorokin.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class TaskPoller {
    private final TaskRepository taskRepository;
    private final TaskPollerProperties taskPollerProperties;
    private final TaskProcessor taskProcessor;

    @Scheduled(fixedDelayString = "#{@taskPollerProperties.delay}")
    @Transactional
    public void poll() {
        if (!taskPollerProperties.isEnabled()) {
            return;
        }

        log.info("Starting polling tasks...");
        List<Task> tasks = taskRepository.findAllByStatusIn(Set.of(TaskStatus.NEW, TaskStatus.FAILED_RETRYABLE), Limit.of(taskPollerProperties.getBatchSize()));

        if (tasks.isEmpty()) {
            return;
        }

        log.info("successfully picked tasks size = [{}]", tasks.size());
        tasks.forEach(task -> task.setStatus(TaskStatus.IN_PROGRESS));
        taskRepository.saveAll(tasks);
        processTasks(tasks);
    }

    private void processTasks(List<Task> tasks) {
        log.info("Starting processing tasks...");
        tasks.forEach(taskProcessor::process);
        log.info("All tasks were processed.");
    }
}
