/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.testng.annotations.Test;

import com.google.common.base.Stopwatch;
import com.lesfurets.reactive.model.QuoteRequest;
import com.lesfurets.reactive.model.QuoteResult;

public class TestSequentialScatterGather extends OrchestratorTest {

    @Test
    public void testSequential() throws Exception {
        Stopwatch watch = Stopwatch.createStarted();
        List<QuoteResult> resultList = LongStream.range(0, 11).sequential()
                        .boxed()
                        .map(l -> new QuoteRequest(l, 0.0))
                        .flatMap(q -> providers.stream().map(p -> p.doReceiveQuote(q)))
                        .collect(Collectors.toList());
        resultList.forEach(r -> System.out.println(r.toString()));
        System.out.println("Elapsed " + watch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");
    }

    @Test
    public void testParallel() throws Exception {
        Stopwatch watch = Stopwatch.createStarted();
        List<QuoteResult> resultList = LongStream.range(0, 11).parallel()
                        .boxed()
                        .map(l -> new QuoteRequest(l, 0.0))
                        .flatMap(q -> providers.stream().map(p -> p.doReceiveQuote(q)))
                        .collect(Collectors.toList());
        resultList.forEach(r -> System.out.println(r.toString()));
        System.out.println("Elapsed " + watch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");
    }

    @Test
    public void testDoubleParallel() throws Exception {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "10");
        Stopwatch watch = Stopwatch.createStarted();
        List<QuoteResult> resultList = LongStream.range(0, 11).parallel()
                        .boxed()
                        .map(l -> new QuoteRequest(l, 0.0))
                        .flatMap(q -> providers.stream().parallel().map(p -> p.doReceiveQuote(q)))
                        .collect(Collectors.toList());
        resultList.forEach(r -> System.out.println(r.toString()));
        System.out.println(ForkJoinPool.commonPool().getPoolSize());
        System.out.println("Elapsed " + watch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");
    }
}
