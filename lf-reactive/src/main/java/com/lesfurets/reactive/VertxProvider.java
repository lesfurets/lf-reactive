/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import com.lesfurets.reactive.model.*;

import io.vertx.core.*;

public class VertxProvider {

    Provider provider;
    WorkerExecutor executor;

    public VertxProvider(Provider provider, WorkerExecutor executor) {
        this.provider = provider;
        this.executor = executor;
    }

    public Future<QuoteResult> doReceiveQuoteAsync(QuoteRequest request) {
        Future<QuoteResult> future = Future.future();
        executor.executeBlocking(ar -> ar.complete(provider.doReceiveQuote(request)), false, future.completer());
        return future;
    }
}
