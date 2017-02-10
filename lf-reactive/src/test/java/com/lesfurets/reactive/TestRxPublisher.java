/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import com.lesfurets.reactive.model.*;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class TestRxPublisher {

    Consumer<QuoteRequest> consumer = q -> {
        System.out.println(q + " - " + Thread.currentThread().getName());
        if (q.getOfferId() == 50) {
            throw new IllegalArgumentException("ERROR");
        }
        int l = (q.getOfferId() % 5 == 0) ? 1500 : 1000;
        try {
            Thread.sleep(l);
        } catch (InterruptedException e1) {
            throw new RuntimeException(e1);
        }
    };

    void runQuote(QuoteRequest q) throws Exception {
        consumer.accept(q);
    }

    void warn(String message){
        System.out.println("WARN " + message);
    }

    @Test
    public void testPublisher() throws Exception {

        PublishProcessor<QuoteRequest> processor = PublishProcessor.create();

        Executors.newFixedThreadPool(10);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        ExecutorSubscriber<QuoteRequest> subscriber = new ExecutorSubscriber<>(executor);
        Scheduler scheduler = Schedulers.from(executor);
        processor
                .onBackpressureDrop(quote -> warn("Drop " + quote.getOfferId()))
                .observeOn(Schedulers.computation(), false, 5)
                .flatMap(q -> Flowable.fromCallable(() -> {
                            runQuote(q);
                            return q;
                        }).subscribeOn(scheduler)
                                .timeout(1000, TimeUnit.MILLISECONDS, Schedulers.computation())
                                .onErrorReturn(throwable -> {
                                    warn("Error " + throwable);
                                    throwable.printStackTrace();
                                    return q;
                                })
                        , false, 10, 5)
                .subscribeWith(subscriber);

        AtomicInteger a = new AtomicInteger();
        for (int i = 0; i < 1_000_000; i++) {
            new Thread(() -> {
                int t = a.incrementAndGet();
                System.out.println("START:  "+t + " - " + Thread.currentThread().getName());
                processor.onNext(new QuoteRequest(t, 0d));
                subscriber.requestIfNeeded();
            }).start();
            try {

                Thread.sleep(75);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Thread.sleep(100_000);


    }

    public static class ExecutorSubscriber<T> implements Subscriber<T> {



        public ExecutorSubscriber(ThreadPoolExecutor executor) {
            this.e = executor;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.s = s;
            s.request(1);
        }

        @Override
        public void onNext(T o) {
            //            s.request(1);
            System.out.println("FINISH: " + o +" - "+ Thread.currentThread().getName());
        }

        @Override
        public void onError(Throwable t) {
            System.out.println("ERROR " + Thread.currentThread().getName());
            t.printStackTrace();
        }

        @Override
        public void onComplete() {

        }

        Subscription s;
        ThreadPoolExecutor e;

        public void requestIfNeeded() {
            if (e.getActiveCount() < 10)
                s.request(1);
        }
    }
}
