package com.lesfurets.reactive.flow;

import java.util.Collection;
import java.util.concurrent.Flow;

public class GatherSubscriber<T> implements Flow.Subscriber<T> {

    Long request;

    Collection<T> results;
    private Flow.Subscription subscription;

    public GatherSubscriber(Long request, Collection<T> list) {
        this.request = request;
        this.results = list;
    }

    public GatherSubscriber(Collection<T> list) {
        this(1L, list);
    }

    public static GatherSubscriber create(Collection results) {
        return new GatherSubscriber(results);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(request);
    }

    @Override
    public void onNext(T next) {
        this.results.add(next);
        subscription.request(1);
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