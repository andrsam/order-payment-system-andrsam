package dev.sorokin.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.task-processor")
public class TaskProcessorProperties {
    private int threadsCount;
    private int maxAttempts;
}
