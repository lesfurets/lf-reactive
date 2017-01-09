/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import com.lesfurets.reactive.flow.*;
import com.lesfurets.reactive.model.QuoteRequest;
import com.lesfurets.reactive.model.QuoteResult;

public class TestReactiveProcessor extends OrchestratorTest {

    ExecutorService e = Executors.newFixedThreadPool(5);

    @Test
    public void testName() throws Exception {
        AtomicLong i = new AtomicLong();
        PeriodicPublisher<QuoteRequest> periodic = new PeriodicPublisher<>(e, 20,
                        () -> new QuoteRequest(i.incrementAndGet(), 0.0), getRequestFrequency(), TimeUnit.MILLISECONDS);

        List<TransformProcessor<QuoteRequest, QuoteResult>> collect = providers.stream()
                        .map(p -> new TransformProcessor<>(executorService, 20, p::doReceiveQuote))
//                        .map(t -> periodic.subscribe(t))
                        .collect(Collectors.toList());

        PrinterSubscriber<QuoteResult> printer = new PrinterSubscriber<>(Long.MAX_VALUE);
        collect.forEach(t -> t.subscribe(printer));
        collect.forEach(periodic::subscribe);

        Thread.sleep(getWaitingTime());
        collect.forEach(SubmissionPublisher::close);

    }
}
