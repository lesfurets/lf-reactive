/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive.flow;

import java.util.concurrent.Flow;

public class PrinterSubscriber<T> implements Flow.Subscriber<T> {

    Long request;

    public PrinterSubscriber(Long request) {
        this.request = request;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(request);
    }

    @Override
    public void onNext(T next) {
        System.out.println("Next : " + Thread.currentThread().getName() + " : " + next);
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
        System.out.println("Complete");
    }
}
