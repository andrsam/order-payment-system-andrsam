package dev.sorokin.repository;

import dev.sorokin.domain.Task;
import dev.sorokin.enums.TaskStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByStatusIn(Collection<TaskStatus> status, Limit limit);
}
