package com.lesfurets.reactive;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class TestRxJava {

    @Test
    public void testPublisher() throws Exception {

        PublishProcessor<Integer> processor = PublishProcessor.create();

        Consumer<Integer> consumer = e -> {
            System.out.println(e + " - " + Thread.currentThread().getName());
            if (e == 50) {
                throw new IllegalArgumentException("ERROR");
            }
            int l = (e % 5 == 0) ? 1500 : 1000;
            try {
                Thread.sleep(l);
            } catch (InterruptedException e1) {
                throw new RuntimeException(e1);
            }
        };

        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        ExecutorSubscriber<Integer> subscriber = new ExecutorSubscriber<>(executor);
        Scheduler scheduler = Schedulers.from(executor);
        processor
//                .compose(FlowableTransformers.onBackpressureTimeout(1,
//                        300, TimeUnit.MILLISECONDS, io.reactivex.schedulers.Schedulers.computation(),
//                        i -> System.out.println("DROP "+ i)))
                .onBackpressureDrop(integer -> System.out.println("Drop " + integer))
                .observeOn(Schedulers.computation(), false, 5)
                .flatMap(i -> Flowable.fromCallable(() -> {
                            consumer.accept(i);
                            return i;
                        }).subscribeOn(scheduler)
                                .timeout(1000, TimeUnit.MILLISECONDS, Schedulers.computation())
                        .onErrorReturn(throwable -> {
                            System.out.println("COMPENSATING " + Thread.currentThread().getName());
                            throwable.printStackTrace();
                            return i;
                        })
                        , false, 10, 5)
                .subscribeWith(subscriber);

        AtomicInteger a = new AtomicInteger();
        for (int i = 0; i < 1_000_000; i++) {
            new Thread(() -> {
                int t = a.incrementAndGet();
                System.out.println("START:  "+t + " - " + Thread.currentThread().getName());
                processor.onNext(t);
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

        Subscription s;
        ThreadPoolExecutor e;

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

        public void requestIfNeeded() {
            int size = e.getQueue().size();
            System.out.println("Queue Size : " + size);
            if (e.getActiveCount() < 10)
                s.request(1);
        }
    }
}
