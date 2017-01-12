/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import com.lesfurets.reactive.model.*;

public class QuoteContext {

  private Provider p;
  private QuoteRequest q;
  private QuoteResult result;


  public QuoteContext(Provider p, QuoteRequest q) {
    this.p = p;
    this.q = q;
  }

  public Provider getP() {
    return p;
  }

  public QuoteRequest getQ() {
    return q;
  }

  public QuoteResult getResult() {
    return result;
  }

  public void setP(Provider p) {
    this.p = p;
  }

  public void setQ(QuoteRequest q) {
    this.q = q;
  }

  public void setResult(QuoteResult result) {
    this.result = result;
  }
}
