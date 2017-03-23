/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

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

    QuoteRequest receiveQuote(QuoteRequest q) throws Exception {
        consumer.accept(q);
        return q;
    }

    void warn(String message){
        System.out.println("WARN " + message);
    }

    void warn(String message, Throwable t){
        System.out.println("WARN " + message);
        t.printStackTrace();
    }

    @Test
    public void testPublisher() throws Exception {

        PublishProcessor<QuoteRequest> processor = PublishProcessor.create();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 100, 0L, MILLISECONDS, new LinkedBlockingQueue<>());
        ExecutorSubscriber<QuoteRequest> subscriber = new ExecutorSubscriber<>(executor);
        Scheduler scheduler = Schedulers.from(executor);
        processor
                .onBackpressureDrop(quote -> warn("Drop " + quote.getOfferId()))
                .observeOn(Schedulers.computation(), false, 5)
                .flatMap(q -> Flowable.fromCallable(() -> receiveQuote(q))
                                .subscribeOn(scheduler)
                                .timeout(1000, MILLISECONDS, Schedulers.computation())
                                .onErrorReturn(throwable -> handleError(q, throwable))
                        , false, 100, 1)
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

    private QuoteRequest handleError(QuoteRequest q, Throwable throwable) {
        warn("Error " + throwable.getMessage(), throwable);
        return q;
    }

    public static class ExecutorSubscriber<T> implements Subscriber<T> {

        AtomicInteger c = new AtomicInteger(0);

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
            c.decrementAndGet();
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
            if (c.get() < 10) {
                System.out.println("THREAD C " + e.getActiveCount());
                c.incrementAndGet();
                s.request(1);
            }
        }
    }
}
