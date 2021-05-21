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

import com.artipie.rpm.Digest;
import com.artipie.rpm.RpmMetadata;
import com.artipie.rpm.meta.XmlPackage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cactoos.map.MapEntry;
import org.cactoos.scalar.Unchecked;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
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
 * Benchmark for {@link RpmMetadata.Append}.
 * @since 1.4
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle DesignForExtensionCheck (500 lines)
 * @checkstyle JavadocMethodCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 20)
public class RpmMetadataAppendBench {

    /**
     * Benchmark directory.
     */
    private static final String BENCH_DIR = System.getenv("BENCH_DIR");

    /**
     * Benchmark metadata.
     */
    private Map<XmlPackage, byte[]> items;

    /**
     * Benchmark rpms.
     */
    private Map<Path, String> rpms;

    @Setup
    public void setup() throws IOException {
        if (RpmMetadataAppendBench.BENCH_DIR == null) {
            throw new IllegalStateException("BENCH_DIR environment variable must be set");
        }
        try (Stream<Path> files = Files.list(Paths.get(RpmMetadataAppendBench.BENCH_DIR))) {
            final List<Path> flist = files.collect(Collectors.toList());
            this.items = flist.stream().map(
                file -> new XmlPackage.Stream(true).get()
                    .filter(xml -> file.toString().contains(xml.filename()))
                    .findFirst().map(
                        item -> new MapEntry<>(
                            item,
                            new Unchecked<>(() -> Files.readAllBytes(file)).value()
                        )
                    )
            ).filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toMap(MapEntry::getKey, MapEntry::getValue));
            this.rpms = flist.stream().filter(item -> item.endsWith(".rpm"))
                .collect(Collectors.toMap(item -> item, item -> item.getFileName().toString()));
        }
    }

    @Benchmark
    public void run(final Blackhole bhl) throws IOException {
        new RpmMetadata.Append(
            Digest.SHA256,
            this.items.entrySet().stream()
            .map(
                entry -> new RpmMetadata.MetadataItem(
                    entry.getKey(),
                    new ByteArrayInputStream(entry.getValue()),
                    new ByteArrayOutputStream()
                )
            ).toArray(RpmMetadata.MetadataItem[]::new)
        ).perform(this.rpms);
    }

    /**
     * Main.
     * @param args CLI args
     * @throws RunnerException On benchmark failure
     */
    public static void main(final String... args) throws RunnerException {
        new Runner(
            new OptionsBuilder()
                .include(RpmMetadataAppendBench.class.getSimpleName()).forks(1).build()
        ).run();
    }

}
