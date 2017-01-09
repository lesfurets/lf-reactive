/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.lesfurets.reactive.model.*;

public class CompletableProvider {

    Provider provider;

    ExecutorService executor;

    public CompletableProvider(Provider provider, ExecutorService executor) {
        this.provider = provider;
        this.executor = executor;
    }

    public CompletableFuture<QuoteResult> doReceiveQuoteAsync(QuoteRequest request) {
        return CompletableFuture.supplyAsync(() -> provider.doReceiveQuote(request), executor);
    }
}
