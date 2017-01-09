/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive.flow;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Function;

public class TransformProcessor<S, T> extends SubmissionPublisher<T> implements Flow.Processor<S, T> {

    private final Function<? super S, ? extends T> function;
    private Flow.Subscription subscription;

    public TransformProcessor(Executor executor, int maxBufferCapacity,
                    Function<? super S, ? extends T> function) {
        super(executor, maxBufferCapacity);
        this.function = function;
    }

    public void onSubscribe(Flow.Subscription subscription) {
        (this.subscription = subscription).request(1);
    }

    public void onNext(S item) {
        subscription.request(1);
        submit(function.apply(item));
    }

    public void onError(Throwable ex) {
        closeExceptionally(ex);
    }

    public void onComplete() {
        close();
    }
}
