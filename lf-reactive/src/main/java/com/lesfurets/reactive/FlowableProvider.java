/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import java.util.concurrent.*;

import com.lesfurets.reactive.model.*;

import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;

public class FlowableProvider {

    Provider provider;
    ExecutorService executor;

    public FlowableProvider(Provider provider, ExecutorService executor) {
        this.provider = provider;
        this.executor = executor;
    }

    private Flowable<QuoteResult> flowableCreate(QuoteRequest request) {
        return Flowable.<QuoteResult> create(s -> {
            s.onNext(provider.doReceiveQuote(request));
            s.onComplete();
        }, BackpressureStrategy.ERROR)
                        .subscribeOn(Schedulers.from(executor))
                        .onBackpressureDrop(q -> System.out.println("Dropped " + q))
                        ;
    }

    public Flowable<QuoteResult> doReceiveQuoteAsync(QuoteRequest request) {
        return Flowable.defer(() -> Flowable.just(provider.doReceiveQuote(request)))
                        .subscribeOn(Schedulers.from(executor))
                        ;
    }

    public Flowable<QuoteResult> doReceiveQuoteAsync(QuoteRequest request,
                                                     FlowableTransformer<QuoteRequest, QuoteRequest> t) {
        return Flowable.just(request)
                .compose(t)
                .map(q -> provider.doReceiveQuote(q))
                .subscribeOn(Schedulers.from(executor))
                        ;
    }

}
