/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import static java.util.stream.Collectors.toList;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import org.testng.annotations.Test;

import com.google.common.base.Stopwatch;
import com.lesfurets.reactive.model.QuoteRequest;
import com.lesfurets.reactive.model.QuoteResult;

import io.reactivex.BackpressureOverflowStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class TestRxFlowable extends OrchestratorTest {

    @Test
    public void testFlowable() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<Flowable<List<QuoteResult>>> merged = IntStream.range(0, 11)
                        .boxed()
                        .map(l -> new QuoteRequest(l, 0.0))
                        .map(this::requestFromAll)
                        .collect(toList());

        System.out.println("Elapsed " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");

        List<QuoteResult> results = Flowable.merge(merged).toList()
                        .blockingGet().stream()
                        .flatMap(Collection::stream)
                        .collect(toList());

        results.forEach(r -> System.out.println(Thread.currentThread().getName() + " -> " + r.toString()));
        System.out.println("Elapsed " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");
        analyzeResults(results);
    }

  private Flowable<List<QuoteResult>> requestFlowable(QuoteRequest q) {
      List<Flowable<QuoteResult>> timeout = providers.stream()
                      .map(s -> new FlowableProvider(s, executorService))
                      .map(p -> p.doReceiveQuoteAsync(q)
                                                      .timeout(getTimeout(), TimeUnit.MILLISECONDS,
                                                                      Flowable.just(new QuoteResult(new TimeoutException("Timeout"), q.getOfferId(), p.provider.getId())))
//            .onBackpressureDrop(quoteRequest -> System.out.println(Thread.currentThread().getName() + " Dropped " + quoteRequest.offerId))
//          .onBackpressureBuffer(100, () -> System.out.println(Thread.currentThread().getName()+": Buffer Overflow "), BackpressureOverflowStrategy.DROP_OLDEST)
                      ).collect(toList());
      return Flowable.merge(timeout).toList().toFlowable();
  }

  private Flowable<List<QuoteResult>> requestFromAll(QuoteRequest q) {
      return Flowable.fromIterable(providers)
                      .map(s -> new FlowableProvider(s, executorService))
                      .flatMap(p -> p.doReceiveQuoteAsync(q)
                                      .timeout(getTimeout(), TimeUnit.MILLISECONDS, Flowable.just(new QuoteResult(new
                                                      TimeoutException("Timeout"), q.getOfferId(), p.provider.getId())))
                      ).toList().toFlowable()
                      ;
  }

    private Flowable<QuoteResult> request(QuoteRequest q, FlowableProvider p) {
        return p.doReceiveQuoteAsync(q)
                        .timeout(getTimeout(), TimeUnit.MILLISECONDS,
                                        Flowable.just(new QuoteResult(new TimeoutException("Timeout"), q.getOfferId(), p.provider.getId())));
    }

  @Test
  public void testFlow() throws Exception {
      final AtomicBoolean running = new AtomicBoolean(true);
      List<QuoteResult> results = new ArrayList<>();
      PublishProcessor<Long> processor = PublishProcessor.create();

      Disposable subscribe = processor
                      .onBackpressureBuffer(100, () -> System.out.println(Thread.currentThread().getName() + ": Buffer Overflow "), BackpressureOverflowStrategy.DROP_OLDEST)
//                      .onBackpressureDrop(quoteRequest -> System.out.println(Thread.currentThread().getName() + " Dropped " + quoteRequest))
                      .observeOn(Schedulers.from(executorService))
                      .map(l -> new QuoteRequest(l, 0.0))
                      .flatMap(this::requestFromAll)
                      .subscribe(results::addAll);

      Flowable.interval(getRequestFrequency(), TimeUnit.MILLISECONDS, Schedulers.io())
                      .takeWhile(t -> running.get())
                      .subscribe(processor);

      Thread.sleep(getWaitingTime());
      running.set(false);
      subscribe.dispose();
      analyzeResults(results);
  }

  @Test
  public void testScatterFlow() throws Exception {
      final AtomicBoolean running = new AtomicBoolean(true);
      List<QuoteResult> results = new ArrayList<>();

      PublishProcessor<QuoteRequest> orchestrator = PublishProcessor.create();

      List<Flowable<QuoteResult>> flowables = providers.stream()
                      .map(p -> {
                          PublishProcessor<QuoteRequest> pp = PublishProcessor.create();
                          orchestrator.subscribe(pp);
                          return pp
                                          .onBackpressureBuffer(10, () -> System.out.println(Thread.currentThread().getName() + ": Buffer Overflow "), BackpressureOverflowStrategy.DROP_OLDEST)
                                          .observeOn(Schedulers.from(executorService))
                                          .flatMap(r -> this.request(r, new FlowableProvider(p, executorService)));
                      }).collect(toList());

      Disposable subscribe = Flowable.merge(flowables)
                      .onBackpressureBuffer(10, () -> System.out.println(Thread.currentThread().getName() + ": Buffer Overflow "), BackpressureOverflowStrategy.DROP_OLDEST)
                      .observeOn(Schedulers.from(executorService))
                      .subscribe(results::add);

      Flowable.interval(getRequestFrequency(), TimeUnit.MILLISECONDS, Schedulers.io())
                      .takeWhile((t) -> running.get())
                      .map(l -> new QuoteRequest(l, 0.0))
                      .doOnComplete(() -> System.out.println("Completed!"))
                      .subscribe(orchestrator);

      Thread.sleep(getWaitingTime());
      running.set(false);
      subscribe.dispose();
      Thread.sleep(100);
      analyzeResults(results);
  }

    @Test
    public void testFlow2() throws Exception {
        final AtomicBoolean running = new AtomicBoolean(true);
        List<QuoteResult> results = new ArrayList<>();
        PublishProcessor<Long> processor = PublishProcessor.create();

        processor
                        .map(l -> new QuoteRequest(l, 0.0))
                        .flatMap(q -> Flowable.fromIterable(providers)
                                        .map(p -> p.doReceiveQuote(q))
                                        .subscribeOn(Schedulers.from(executorService))
                                        .timeout(getTimeout(), TimeUnit.MILLISECONDS, Flowable.just(new QuoteResult(new TimeoutException("Timeout"), q.getOfferId(), -1))))
        .onBackpressureDrop(qr -> System.out.println("Dropping " + qr.getProviderId()))
                        .subscribe(results::add);

        Flowable.interval(getRequestFrequency(), TimeUnit.MILLISECONDS, Schedulers.io())
                        .takeWhile(t -> running.get())
                        .subscribe(processor);

        Thread.sleep(getWaitingTime());
        running.set(false);
        Thread.sleep(100);
        analyzeResults(results);
    }

    @Override
    protected int getPoolSize() {
        return 10;
    }
}
