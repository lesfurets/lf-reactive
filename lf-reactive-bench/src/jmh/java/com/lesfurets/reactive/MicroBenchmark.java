/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.reactive;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class MicroBenchmark {

    @Benchmark
    @BenchmarkMode(Mode.All)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Fork(1)
    @Warmup(iterations = 5)
    @Measurement(iterations = 5)
    public void measureThroughput() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(1000);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                        .include(MicroBenchmark.class.getSimpleName())
                        .warmupIterations(5)
                        .measurementIterations(5)
                        .forks(1)
                        .build();

        new Runner(opt).run();
    }
}
