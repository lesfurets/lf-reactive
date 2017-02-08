package com.lesfurets.reactive;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.lesfurets.reactive.model.Provider;
import com.lesfurets.reactive.model.QuoteResult;

public class OrchestratorTest {

    protected int poolSize = 10;

    protected ExecutorService executorService;

    protected Set<Provider> providers = Set.of(
                    new Provider(1, 0.0),
                    new Provider(2, 0.0),
                    new Provider(3, 0.0),
                    new Provider(4, 0.0)
    );

    @BeforeMethod
    void setUpTest() {
        executorService = Executors.newFixedThreadPool(getPoolSize());
    }

    protected int getPoolSize() {
        return poolSize;
    }

    @AfterMethod
    void cleanTest() {
        if (executorService != null)
            executorService.shutdown();
    }

    void analyzeResults(Collection<QuoteResult> results) {
        Map<Long, List<QuoteResult>> collect = results.stream()
                        .filter(QuoteResult::hasError)
                        .sorted()
                        .collect(groupingBy(q -> q.getProviderId()));
        collect.entrySet().forEach(System.out::println);
        System.out.println(results.stream()
                        .filter(QuoteResult::hasError)
                        .filter(q -> TimeoutException.class.equals(q.getErrorCause().getClass()))
                        .count() + "/" + results.size());
        System.out.println("Gathered " + results.stream().filter(q -> !q.hasError()).count() + " quotes.");
        results.forEach(System.out::println);
        Map<Long, Long> providerQuotes = results.stream()
                        .filter(q -> !q.hasError())
                        .collect(groupingBy(q -> q.getProviderId(), counting()));
        providerQuotes.entrySet().forEach(e -> System.out.println("Provider " + e.getKey() + " : " + e.getValue()));
    }

    protected int getTimeout() {
        return 2000;
    }

    protected int getWaitingTime() {
        return 5000;
    }

    protected int getRequestFrequency() {
        return 200;
    }

    protected void benchmark(AtomicBoolean running,
                             List<QuoteResult> results,
                             Function<Long, List<QuoteResult>> handleRequest)
            throws InterruptedException {
        System.out.println("Starting");
        ExecutorService executorService = Executors.newCachedThreadPool();
        final AtomicLong l = new AtomicLong(0L);
        new Thread(() -> {
            while (running.get()) {
                executorService.execute(() -> {
                    List<QuoteResult> resultList = handleRequest.apply(l.get());
//                    System.out.println(resultList);
                    results.addAll(resultList);
                });
                try {
                    Thread.sleep(getRequestFrequency());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                l.incrementAndGet();
            }
        }).start();

        System.out.println("Started");
        Thread.sleep(getWaitingTime());
        running.set(false);
        Thread.sleep(100);
        executorService.shutdownNow();
        System.out.println("# of requests: "+l.get());
        analyzeResults(results);
    }
}