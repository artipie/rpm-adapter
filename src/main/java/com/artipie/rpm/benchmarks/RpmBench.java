/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artipie.rpm.benchmarks;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.rpm.Rpm;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Benchmark for {@link RPM}.
 * @since 1.4
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle DesignForExtensionCheck (500 lines)
 * @checkstyle JavadocMethodCheck (500 lines)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 20)
public class RpmBench {

    /**
     * Benchmark directory.
     */
    private static final String BENCH_DIR = System.getenv("BENCH_DIR");

    /**
     * Repository storage.
     */
    private Storage storage;

    @Setup
    public void setup() {
        if (RpmBench.BENCH_DIR == null) {
            throw new IllegalStateException("BENCH_DIR environment variable must be set");
        }
        this.storage = new InMemoryStorage();
        final Storage src = new FileStorage(Paths.get(RpmBench.BENCH_DIR));
        RpmBench.sync(src, this.storage);
    }

    @Setup(Level.Iteration)
    public void setupIter() {
        final RxStorageWrapper rxst = new RxStorageWrapper(this.storage);
        rxst.list(new Key.From("repodata"))
            .flatMapObservable(Observable::fromIterable)
            .flatMapCompletable(key -> rxst.delete(key))
            .to(CompletableInterop.await()).toCompletableFuture().join();
    }

    @Benchmark
    public void run(final Blackhole bhl) {
        new Rpm(this.storage).batchUpdateIncrementally(Key.ROOT)
            .to(CompletableInterop.await())
            .toCompletableFuture().join();
    }

    /**
     * Main.
     * @param args CLI args
     * @throws RunnerException On benchmark failure
     */
    public static void main(final String... args) throws RunnerException {
        new Runner(
            new OptionsBuilder()
                .include(RpmBench.class.getSimpleName())
                .forks(1)
                .build()
        ).run();
    }

    /**
     * Sync storages.
     * @param src Source storage
     * @param dst Destination storage
     */
    private static void sync(final Storage src, final Storage dst) {
        Single.fromFuture(src.list(Key.ROOT))
            .flatMapObservable(Observable::fromIterable)
            .flatMapSingle(
                key -> Single.fromFuture(
                    src.value(key)
                        .thenCompose(content -> dst.save(key, content))
                        .thenApply(none -> true)
                )
            ).toList().map(ignore -> true).to(SingleInterop.get())
                .toCompletableFuture().join();
    }
}
