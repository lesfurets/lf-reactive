/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import java.util.concurrent.*;

import com.lesfurets.reactive.model.*;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class ObservableProvider {

    Provider provider;
    ExecutorService executor;

    public ObservableProvider(Provider provider, ExecutorService executor) {
        this.provider = provider;
        this.executor = executor;
    }

    public Observable<QuoteResult> doReceiveQuoteAsync(QuoteRequest request) {
        return Observable.<QuoteResult> create(s -> {
            s.onNext(provider.doReceiveQuote(request));
            s.onComplete();
        }).subscribeOn(Schedulers.from(executor));
    }

}
