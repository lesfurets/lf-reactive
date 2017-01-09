/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import org.testng.annotations.Test;

import com.google.common.base.Stopwatch;
import com.lesfurets.reactive.model.QuoteRequest;
import com.lesfurets.reactive.model.QuoteResult;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class TestRxObservable extends OrchestratorTest {

    @Test
    public void testObservable() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<Observable<QuoteResult>> resultList = IntStream.range(0, 11).parallel()
                        .boxed()
                        .map(l -> new QuoteRequest(l, 0.0))
                        .flatMap(q -> providers.stream()
                                        .map(p -> new ObservableProvider(p, executorService))
                                        .map(p -> p.doReceiveQuoteAsync(q)
                                                        .timeout(getTimeout(), TimeUnit.MILLISECONDS,
                                                                        Observable.just(new QuoteResult(new TimeoutException("Timeout"), q.getOfferId(), p.provider.getId())))))
                        .collect(toList());

        System.out.println("Elapsed " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");

        Single<List<QuoteResult>> merged = Observable.merge(resultList).toList();

        List<QuoteResult> results = merged.blockingGet();
        results.forEach(r -> System.out.println(Thread.currentThread().getName() + " -> " +r.toString()));
        System.out.println("Elapsed " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");
        analyzeResults(results);
    }

    @Test
    public void testParallelObservable() throws Exception {

        Stopwatch stopwatch = Stopwatch.createStarted();

        Observable<Long> vals = Observable.rangeLong(0, 10);

        Single<List<QuoteResult>> single = vals
                        .map(l -> new QuoteRequest(l, 0.0))
                        .flatMap(q -> Observable.fromIterable(providers)
                                        .map(p -> p.doReceiveQuote(q))
                                        .subscribeOn(Schedulers.from(executorService))
                                        .timeout(2000, TimeUnit.MILLISECONDS, Observable.just(new QuoteResult(new TimeoutException("Timeout"),q.getOfferId(), -1)))
                        ).toList();

        System.out.println("Elapsed " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");

        List<QuoteResult> results = single.blockingGet();
        results.forEach(r -> System.out.println(Thread.currentThread().getName() + " -> " + r.toString()));
        System.out.println("Elapsed " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");
        analyzeResults(results);
    }
}
