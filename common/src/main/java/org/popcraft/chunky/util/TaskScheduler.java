package org.popcraft.chunky.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {
    private final ExecutorService executor;
    private final Set<Future<?>> futures = ConcurrentHashMap.newKeySet();

    public TaskScheduler() {
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        final int coreThreads = Input.tryInteger(System.getProperty("chunky.schedulerCoreThreads"))
                .orElse(Math.max(3, availableProcessors / 2));
        final int maxThreads = Input.tryInteger(System.getProperty("chunky.schedulerMaxThreads"))
                .orElse(Math.max(10, availableProcessors * 2));
        final int queueCapacity = Input.tryInteger(System.getProperty("chunky.schedulerQueueSize"))
                .orElse(200);

        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                coreThreads,
                maxThreads,
                5,                              // keepAliveTime
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(queueCapacity)
        );
        threadPoolExecutor.setThreadFactory(runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        threadPoolExecutor.prestartAllCoreThreads();
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        this.executor = threadPoolExecutor;
    }

    public void runTask(final Runnable runnable) {
        futures.add(executor.submit(runnable));
        futures.removeIf(Future::isDone);
    }

    public void cancelTasks() {
        for (Future<?> future : futures) {
            future.cancel(true);
        }
        futures.clear();
    }
}
