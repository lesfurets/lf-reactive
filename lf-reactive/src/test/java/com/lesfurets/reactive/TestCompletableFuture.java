/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import com.google.common.base.Stopwatch;
import com.lesfurets.reactive.flow.GatherSubscriber;
import com.lesfurets.reactive.flow.PeriodicPublisher;
import com.lesfurets.reactive.model.QuoteRequest;
import com.lesfurets.reactive.model.QuoteResult;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class TestCompletableFuture extends OrchestratorTest {

    private ExecutorService loggerE = Executors.newFixedThreadPool(3);

    ArrayList<Long> monitoring = new ArrayList<>();

    private boolean monitor(QuoteResult r) {
        return monitoring.add(r.getQuote().getOfferId());
    }

    @Override
    protected int getPoolSize() {
        return 10;
    }

    @Test
    public void testCompletableFuture() throws Exception {
        Stopwatch watch = Stopwatch.createStarted();
        List<CompletableFuture<QuoteResult>> futures = LongStream.range(0, 11).sequential()
                        .boxed()
                        .map(l -> new QuoteRequest(l, 0.0))
                        .flatMap(q -> providers.stream()
                                        .map(p -> new CompletableProvider(p, executorService))
                                        .map(p -> {
                                            CompletableFuture<QuoteResult> future = p.doReceiveQuoteAsync(q);
                                            future.thenAcceptAsync(this::monitor, loggerE);
                                            return future;
                                        }))
                        .collect(Collectors.toList());

        System.out.println("Elapsed " + watch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");

        CompletableFuture<List<QuoteResult>> resultList =
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                        .thenApply(v -> futures.stream()
                                                        .map(CompletableFuture::join)
                                                        .collect(toList()));

        System.out.println("Elapsed " + watch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");

        resultList.get();
        System.out.println("Elapsed " + watch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");
    }

    @Test
    public void testCompletableFutureWithTimeout() throws Exception {
        Stopwatch watch = Stopwatch.createStarted();
        List<CompletableFuture<QuoteResult>> futures = LongStream.range(0, 11).sequential()
                        .boxed()
                        .map(l -> new QuoteRequest(l, 0.0))
                        .flatMap(q -> providers.stream()
                                        .map(p -> new CompletableProvider(p, executorService))
                                        .map(p -> {
                                            CompletableFuture<QuoteResult> timeout = p.doReceiveQuoteAsync(q)
                                                            .completeOnTimeout(new QuoteResult(new TimeoutException
                                                                                            ("Timeout"), q.getOfferId
                                                                                            (), p.provider.getId()),
                                                                            getTimeout(), TimeUnit.MILLISECONDS);
                                            timeout.thenApplyAsync(this::monitor, loggerE);
                                            return timeout;
                                        })
                        )
                        .collect(Collectors.toList());

        System.out.println("Elapsed " + watch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");

        CompletableFuture<List<QuoteResult>> resultList =
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                        .thenApply(v -> futures.stream()
                                                        .map(CompletableFuture::join)
                                                        .collect(toList()));

        System.out.println("Elapsed " + watch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");

        List<QuoteResult> rl = resultList.get();
        rl.forEach(q -> System.out.println(Thread.currentThread().getName() + " -> " + q.toString()));
        System.out.println("Elapsed " + watch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");
        System.out.println(monitoring);
        analyzeResults(rl);
    }

    @Test
    public void testCompletableFutureFlow() throws Exception {
        Stopwatch watch = Stopwatch.createStarted();

        final AtomicBoolean running = new AtomicBoolean(true);
        ArrayList<QuoteResult> results = new ArrayList<>();
        final AtomicLong atomicLong = new AtomicLong(0);

        PeriodicPublisher<Long> periodicPublisher = new PeriodicPublisher<>(loggerE, Integer.MAX_VALUE,
                        atomicLong::incrementAndGet, getRequestFrequency(), TimeUnit.MILLISECONDS);
        providers.stream()
                        .map(p -> new FlowProvider(executorService, p))
                        .forEach(f -> {
                            f.subscribe(GatherSubscriber.<QuoteResult> create(results));
                            periodicPublisher.subscribe(f);
                        });

        System.out.println("Elapsed " + watch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");

        Thread.sleep(getWaitingTime());
        running.set(false);
        Thread.sleep(100);
        analyzeResults(results);

        System.out.println("Elapsed " + watch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");

    }

    @Test
    public void testCompletableFutureRx() throws Exception {
        final AtomicBoolean running = new AtomicBoolean(true);
        List<QuoteResult> results = new ArrayList<>();
        Flowable.interval(getRequestFrequency(), TimeUnit.MILLISECONDS, Schedulers.io())
                        .takeWhile(t -> running.get())
                        .observeOn(Schedulers.io())
                        .subscribe(s -> doTest(s, results));
        Thread.sleep(getWaitingTime());
        running.set(false);
        analyzeResults(results);
    }

    private void doTest(Long s, List<QuoteResult> results) throws ExecutionException, InterruptedException {
        List<CompletableFuture<QuoteResult>> futures = Stream.of(s)
                        .map(l -> new QuoteRequest(l, 0.0))
                        .flatMap(q -> providers.stream()
                                        .map(p -> new CompletableProvider(p, executorService))
                                        .map(p -> {
                                            CompletableFuture<QuoteResult> timeout = p.doReceiveQuoteAsync(q)
                                                            .completeOnTimeout(new QuoteResult(new TimeoutException
                                                                                            ("Timeout"), q.getOfferId
                                                                                            (), p.provider.getId()),
                                                                            getTimeout(), TimeUnit.MILLISECONDS);
                                            timeout.thenApplyAsync(this::monitor, loggerE);
                                            return timeout;
                                        })
                        )
                        .collect(Collectors.toList());

        CompletableFuture<List<QuoteResult>> resultList =
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                        .thenApply(v -> futures.stream()
                                                        .map(CompletableFuture::join)
                                                        .collect(toList()));

        System.out.println("- " + Thread.currentThread().getName());
        results.addAll(resultList.get());
    }
}
