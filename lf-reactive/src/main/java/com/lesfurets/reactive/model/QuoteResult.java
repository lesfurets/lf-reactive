/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive.model;

import java.util.Objects;

public class QuoteResult implements Comparable<QuoteResult> {

    long providerId;

    long offerId;

    Quote quote;

    Throwable errorCause;

    public QuoteResult(Quote quote, long providerId) {
        this.quote = quote;
        this.offerId = quote.offerId;
        this.providerId = providerId;
    }

    public QuoteResult(Throwable errorCause, long offerId, long providerId) {
        this.errorCause = errorCause;
        this.offerId = offerId;
        this.providerId = providerId;
    }

    @Override
    public int compareTo(QuoteResult quoteResult) {
        return Math.toIntExact(this.providerId - quoteResult.providerId);
    }

    public Quote getQuote() {
        return quote;
    }

    public void setQuote(Quote quote) {
        this.quote = quote;
    }

    public Throwable getErrorCause() {
        return errorCause;
    }

    public void setErrorCause(Throwable errorCause) {
        this.errorCause = errorCause;
    }

    public long getProviderId() {
        return providerId;
    }

    public void setProviderId(long providerId) {
        this.providerId = providerId;
    }

    public long getOfferId() {
        return offerId;
    }

    public void setOfferId(long offerId) {
        this.offerId = offerId;
    }

    public boolean hasError() {
        return errorCause != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QuoteResult that = (QuoteResult) o;
        return Objects.equals(quote, that.quote) &&
                        Objects.equals(errorCause, that.errorCause);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quote, errorCause);
    }

    @Override
    public String toString() {
        return "QuoteResult{" +
                        "quoteId=" + offerId +
                        ", providerId=" + providerId +
                        ", errorCause=" + (errorCause == null ? null : errorCause.getMessage()) +
                        '}';
    }
}
