package dev.sorokin.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.task-poller")
public class TaskPollerProperties {
    private int batchSize;
    private Duration delay;
    private boolean enabled;
    private int maxRetries;
}
