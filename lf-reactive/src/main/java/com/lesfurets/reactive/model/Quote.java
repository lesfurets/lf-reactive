/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Quote {

    long offerId;

    BigDecimal amount;

    public Quote(long offerId, BigDecimal amount) {
        this.offerId = offerId;
        this.amount = amount;
    }

    public long getOfferId() {
        return offerId;
    }

    public void setOfferId(long offerId) {
        this.offerId = offerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Quote{" +
                        "offerId=" + offerId +
                        ", amount=" + amount +
                        '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Quote quote = (Quote) o;
        return offerId == quote.offerId &&
                        Objects.equals(amount, quote.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offerId, amount);
    }
}
