package com.tobyang.easyeventbus;

import com.tobyang.easyeventbus.guava.AsyncEventBus;
import com.tobyang.easyeventbus.guava.DelayedEventConfig;
import com.tobyang.easyeventbus.guava.EventBus;
import com.tobyang.easyeventbus.guava.EventInterceptor;
import com.tobyang.easyeventbus.guava.InterceptorChain;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * EasyEventBusConfiguration - Spring Boot auto-configuration for EasyEventBus
 * 
 * This class provides the Spring Boot auto-configuration for the EasyEventBus.
 * It is responsible for creating the EventBus bean and enabling it based on
 * the configuration properties.
 * 
 * @author tobyang
 * @version 1.0
 */
@Configuration
@EnableConfigurationProperties(EasyEventBusProperties.class)
@ConditionalOnProperty(prefix = "easyeventbus", name = "enable", havingValue = "true")
public class EasyEventBusConfiguration {

    /**
     * 创建延迟事件配置Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public DelayedEventConfig delayedEventConfig(EasyEventBusProperties properties) {
        EasyEventBusProperties.DelayedEvent delayedConfig = properties.getDelayed();

        if (!delayedConfig.isEnabled()) {
            return DelayedEventConfig.defaultConfig();
        }

        return DelayedEventConfig.builder()
                .corePoolSize(delayedConfig.getCorePoolSize())
                .threadNamePrefix(delayedConfig.getThreadNamePrefix())
                .build();
    }

    /**
     * 创建拦截器链Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public InterceptorChain interceptorChain(List<EventInterceptor> interceptors) {
        return new InterceptorChain(interceptors);
    }

    /**
     * Creates the EventBus bean if not already defined.
     * Creates either a synchronous or asynchronous EventBus based on configuration.
     *
     * @param properties The EasyEventBus configuration properties
     * @param delayedEventConfig The delayed event configuration
     * @return The EventBus instance (either EventBus or AsyncEventBus)
     */
    @Bean
    @ConditionalOnMissingBean
    public EventBus eventBus(EasyEventBusProperties properties, DelayedEventConfig delayedEventConfig) {
        if (properties.isAsyncEnabled()) {
            // Create AsyncEventBus with configured thread pool and proper thread naming
            ThreadFactory threadFactory = new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "EasyEventBus-" +
                                             properties.getIdentifier() + "-" +
                                             threadNumber.getAndIncrement());
                    thread.setDaemon(true); // 设置为守护线程，避免阻止JVM关闭
                    return thread;
                }
            };

            return new AsyncEventBus(
                properties.getIdentifier(),
                Executors.newFixedThreadPool(properties.getAsyncThreadPoolSize(), threadFactory),
                delayedEventConfig
            );
        } else {
            // Create synchronous EventBus (不支持延迟事件)
            return new EventBus(properties.getIdentifier());
        }
    }
}