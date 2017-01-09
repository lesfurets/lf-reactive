package com.lesfurets.reactive;

import com.lesfurets.reactive.flow.TransformProcessor;
import com.lesfurets.reactive.model.*;

import java.util.concurrent.*;

public class FlowProvider extends TransformProcessor<Long, QuoteResult> {

    public FlowProvider(Executor executor, Provider p) {
        super(executor, Flow.defaultBufferSize(),
                        l -> {
                            QuoteResult timeout = new QuoteResult(new TimeoutException("Timeout"), l, p.getId());
                            try {
                                return CompletableFuture
                                                .supplyAsync(() -> p.doReceiveQuote(new QuoteRequest(l, 0.0)), executor)
                                                .completeOnTimeout(timeout, 2000, TimeUnit.MILLISECONDS)
                                                .get();
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                            return timeout;
                        });
    }

}
