package com.agent.memory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 记忆抽取线程池。
 * <p>
 * 抽取属于旁路任务：单独线程池 + 守护线程，既不拖慢对话响应，
 * 也不会阻止应用退出。
 */
@Configuration
public class MemoryAsyncConfig {

    @Bean(name = "memoryExecutor", destroyMethod = "shutdown")
    public ExecutorService memoryExecutor(MemoryProperties properties) {
        AtomicInteger counter = new AtomicInteger();
        return Executors.newFixedThreadPool(
                Math.max(1, properties.getLongterm().getExecutorPoolSize()),
                runnable -> {
                    Thread thread = new Thread(runnable, "memory-extractor-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
    }
}
