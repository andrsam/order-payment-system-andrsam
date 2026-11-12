package dev.sorokin.config;

import dev.sorokin.config.properties.TaskProcessorProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
public class CommonApplicationConfig {
    @Bean
    public StubHttpClient stubHttpClient(RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl("http://localhost:8081")
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(StubHttpClient.class);
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService taskDispatcherThreadPool(TaskProcessorProperties taskProcessorProperties) {
        return Executors.newFixedThreadPool(taskProcessorProperties.getThreadsCount());
    }

}
