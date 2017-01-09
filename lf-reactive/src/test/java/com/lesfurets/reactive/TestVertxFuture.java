/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.lesfurets.reactive.flow.PeriodicPublisher;
import com.lesfurets.reactive.model.QuoteRequest;
import com.lesfurets.reactive.model.QuoteResult;

import io.vertx.core.*;
import io.vertx.core.Future;
import io.vertx.core.streams.Pump;
import io.vertx.ext.reactivestreams.ReactiveReadStream;
import io.vertx.ext.reactivestreams.ReactiveWriteStream;

/**
 * TODO
 */
public class TestVertxFuture extends OrchestratorTest {

    Vertx vertx;

    ExecutorService e = Executors.newFixedThreadPool(5);

    @BeforeMethod
    public void setUp() throws Exception {

        vertx = Vertx.vertx();
        System.out.println("dfasdfs");

    }

    @Test(enabled = false)
    public void testVertx() throws Exception {
        WorkerExecutor something = vertx.createSharedWorkerExecutor("something", 10, 2000);
        something.executeBlocking(event -> {
            System.out.println(Thread.currentThread().getName() + " something starting");
        }, false, event -> {

        });
        List<Future> collect = LongStream.range(0, 11)
                        .boxed()
                        .map(l -> new QuoteRequest(l, 0.0))
                        .flatMap(q -> providers.stream()
                                        .map(p -> new VertxProvider(p, something))
                                        .map(p -> p.doReceiveQuoteAsync(q))
                        ).collect(Collectors.toList());

        vertx.runOnContext(event -> {

        });

        System.out.println("STARTING");

        CompositeFuture join = CompositeFuture.<QuoteResult> join(collect);
        join.setHandler(event -> {
            if (event.succeeded()) {
                List<QuoteResult> list = event.result().list();
                for (QuoteResult result : list) {
                    System.out.println(result.toString());
                }
            }
        });
    }

    @Test(enabled = false)
    public void testPump() throws Exception {

        AtomicLong i = new AtomicLong();
        PeriodicPublisher<QuoteRequest> periodic = new PeriodicPublisher<>(e, 20,
                        () -> new QuoteRequest(i.incrementAndGet(), 0.0), 20, TimeUnit.MILLISECONDS);

        ReactiveReadStream<QuoteRequest> rs = ReactiveReadStream.readStream();

        ReactiveWriteStream<QuoteRequest> ws = ReactiveWriteStream.writeStream(vertx);

        rs.handler(System.out::println);

        Pump pump = Pump.pump(rs, ws);
        pump.start();

    }
}
