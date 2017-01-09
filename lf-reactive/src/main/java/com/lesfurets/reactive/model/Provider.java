/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive.model;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

public class Provider {

    protected long id;
    private double errorRate;
    private BigDecimal decimal = BigDecimal.valueOf(id);

    public Provider(long id, double errorRate) {
        this.id = id;
        this.errorRate = errorRate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = errorRate;
    }

    public QuoteResult doReceiveQuote(QuoteRequest request) {
        Stopwatch started = Stopwatch.createStarted();
        long offerId = request.offerId;
        System.out.println(Thread.currentThread().getName() + " -> " + id+": Receiving quote for " + offerId + " with error rate " + this.errorRate);
        double randomError = ThreadLocalRandom.current().nextDouble();
        try {
            if (id == 1) {
                Thread.sleep(200);
                return new QuoteResult(new Quote(offerId, decimal), id);
            }
            if (errorRate != 0.0 && randomError >= errorRate) {
                Thread.sleep(1000);
                return new QuoteResult(new RuntimeException("Shit happens..."), offerId, id);
            } else if (offerId % 10 == 0) {
                Thread.sleep(3000);
            } else {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e1) {
            return new QuoteResult(e1, offerId, id);
        }
        System.out.println(Thread.currentThread().getName() + " -> "+ id +": Elapsed " + started.elapsed(TimeUnit.MILLISECONDS)+" "+offerId);
        return new QuoteResult(new Quote(offerId, decimal), id);
    }

}
