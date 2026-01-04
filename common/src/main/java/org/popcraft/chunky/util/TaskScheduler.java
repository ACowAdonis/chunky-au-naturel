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
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                3,                              // core threads
                10,                             // max threads (was Integer.MAX_VALUE - unbounded!)
                5,                              // keepAliveTime
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(100)  // bounded queue with capacity 100 (was SynchronousQueue with 0 capacity)
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
