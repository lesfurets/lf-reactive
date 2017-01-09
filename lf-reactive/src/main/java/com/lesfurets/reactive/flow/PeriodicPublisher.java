/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive.flow;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class PeriodicPublisher<T> extends SubmissionPublisher<T> {

    private final ScheduledFuture<?> periodicTask;
    private final ScheduledExecutorService scheduler;

    public PeriodicPublisher(Executor executor, int maxBufferCapacity,
                    Supplier<? extends T> supplier,
                    long period, TimeUnit unit) {
        super(executor, maxBufferCapacity);
        scheduler = new ScheduledThreadPoolExecutor(1);
        periodicTask = scheduler.scheduleAtFixedRate(() -> submit(supplier.get()), 0, period, unit);
    }

    public void close() {
        periodicTask.cancel(false);
        scheduler.shutdown();
        super.close();
    }
}