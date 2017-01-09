/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive.model;

import java.util.Objects;

public class QuoteRequest {

    long offerId;

    double errorRate;

    public QuoteRequest(long offerId, double errorRate) {
        this.offerId = offerId;
        this.errorRate = errorRate;
    }

    public long getOfferId() {
        return offerId;
    }

    public void setOfferId(long offerId) {
        this.offerId = offerId;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = errorRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QuoteRequest that = (QuoteRequest) o;
        return offerId == that.offerId &&
                        Double.compare(that.errorRate, errorRate) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(offerId, errorRate);
    }
}
