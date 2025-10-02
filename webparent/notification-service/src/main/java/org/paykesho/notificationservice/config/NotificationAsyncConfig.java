package org.paykesho.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous notification processing
 */
@Configuration
@EnableAsync
@EnableScheduling
public class NotificationAsyncConfig {

    // Hardcoded configuration values
    private static final int EMAIL_CORE_POOL_SIZE = 2;
    private static final int EMAIL_MAX_POOL_SIZE = 5;
    private static final int EMAIL_QUEUE_CAPACITY = 100;
    private static final int NOTIFICATION_CORE_POOL_SIZE = 1;
    private static final int NOTIFICATION_MAX_POOL_SIZE = 3;
    private static final int NOTIFICATION_QUEUE_CAPACITY = 50;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;

    /**
     * Thread pool executor for email sending tasks
     */
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(EMAIL_CORE_POOL_SIZE);
        executor.setMaxPoolSize(EMAIL_MAX_POOL_SIZE);
        executor.setQueueCapacity(EMAIL_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("EmailSender-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(SHUTDOWN_TIMEOUT_SECONDS);
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool executor for notification processing tasks
     */
    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(NOTIFICATION_CORE_POOL_SIZE);
        executor.setMaxPoolSize(NOTIFICATION_MAX_POOL_SIZE);
        executor.setQueueCapacity(NOTIFICATION_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("NotificationProcessor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(SHUTDOWN_TIMEOUT_SECONDS);
        executor.initialize();
        return executor;
    }
}
