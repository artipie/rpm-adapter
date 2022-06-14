/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

/**
 * Test for SyncRpmStorageValuePipelineTest.
 *
 * @since 0.9
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle AvoidDuplicateLiterals (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class ReactorRpmStorageValuePipelineTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void processesExistingItem() {
        final Key key = new Key.From("test.txt");
        final Charset charset = StandardCharsets.US_ASCII;
        this.asto.save(key, new Content.From("one\ntwo\nfour".getBytes(charset))).join();
        new RxStorageValuePipeline<>(this.asto, key).process(
            (input, out) -> {
                try {
                    final List<String> list = IOUtils.readLines(input.get(), charset);
                    list.add(2, "three");
                    IOUtils.writeLines(list, "\n", out, charset);
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                }
            }
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new String(new BlockingStorage(this.asto).value(key), charset),
            new IsEqual<>("one\ntwo\nthree\nfour\n")
        );
    }

    @ParameterizedTest
    @CsvSource({
        "key_from,key_to",
        "key_from,key_from"
    })
    void processesExistingLargeSizeItem(
        final String read, final String write
    ) {
        final int size = 4 * 1024;
        final int bufsize = 128;
        final byte[] data = new byte[size];
        new Random().nextBytes(data);
        final Key kfrom = new Key.From(read);
        final Key kto = new Key.From(write);
        this.asto.save(kfrom, new Content.From(data)).join();
        new RxStorageValuePipeline<String>(this.asto, kfrom, kto)
            .processWithResult(
                (input, out) -> {
                    final byte[] buffer = new byte[bufsize];
                    try {
                        final InputStream stream = input.get();
                        while (stream.read(buffer) != -1) {
                            IOUtils.write(buffer, out);
                            out.flush();
                        }
                        new Random().nextBytes(buffer);
                        IOUtils.write(buffer, out);
                        out.flush();
                    } catch (final IOException err) {
                        throw new ArtipieIOException(err);
                    }
                    return "res";
                }
            ).thenCompose(
                r -> this.asto.value(kto)
                    .thenApply(
                        cnt -> {
                            MatcherAssert.assertThat(
                                new BlockingStorage(this.asto).value(kto).length,
                                new IsEqual<>(size + bufsize)
                            );
                            return null;
                        }
                    ).thenApply(n -> r)).toCompletableFuture().join();
    }

    @Test
    void writesNewItem() {
        final Key key = new Key.From("my_test.txt");
        final Charset charset = StandardCharsets.US_ASCII;
        final String text = "Hello world!";
        new RxStorageValuePipeline<>(this.asto, key).process(
            (input, out) -> {
                MatcherAssert.assertThat(
                    "Input should be absent",
                    input.isPresent(),
                    new IsEqual<>(false)
                );
                try {
                    IOUtils.write(text, out, charset);
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                }
            }
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "test.txt does not contain text `Hello world!`",
            new String(new BlockingStorage(this.asto).value(key), charset),
            new IsEqual<>(text)
        );
    }

    @Test
    void processesExistingItemAndReturnsResult() {
        final Key key = new Key.From("test.txt");
        final Charset charset = StandardCharsets.US_ASCII;
        this.asto.save(key, new Content.From("five\nsix\neight".getBytes(charset))).join();
        MatcherAssert.assertThat(
            "Resulting lines count should be 4",
            new RxStorageValuePipeline<Integer>(this.asto, key).processWithResult(
                (input, out) -> {
                    try {
                        final List<String> list = IOUtils.readLines(input.get(), charset);
                        list.add(2, "seven");
                        IOUtils.writeLines(list, "\n", out, charset);
                        return list.size();
                    } catch (final IOException err) {
                        throw new ArtipieIOException(err);
                    }
                }
            ).toCompletableFuture().join(),
            new IsEqual<>(4)
        );
        MatcherAssert.assertThat(
            "Storage item was not updated",
            new String(new BlockingStorage(this.asto).value(key), charset),
            new IsEqual<>("five\nsix\nseven\neight\n")
        );
    }

    @Test
    void writesNewItemAndReturnsResult() {
        final Key key = new Key.From("my_test.txt");
        final Charset charset = StandardCharsets.US_ASCII;
        final String text = "Have a food time!";
        MatcherAssert.assertThat(
            new RxStorageValuePipeline<>(this.asto, key).processWithResult(
                (input, out) -> {
                    MatcherAssert.assertThat(
                        "Input should be absent",
                        input.isPresent(),
                        new IsEqual<>(false)
                    );
                    try {
                        IOUtils.write(text, out, charset);
                        return text.getBytes(charset).length;
                    } catch (final IOException err) {
                        throw new ArtipieIOException(err);
                    }
                }
            ).toCompletableFuture().join(),
            new IsEqual<>(17)
        );
        MatcherAssert.assertThat(
            "test.txt does not contain text `Have a food time!`",
            new String(new BlockingStorage(this.asto).value(key), charset),
            new IsEqual<>(text)
        );
    }

    @Test
    void writesToNewLocation() {
        final Key read = new Key.From("read.txt");
        final Key write = new Key.From("write.txt");
        final Charset charset = StandardCharsets.US_ASCII;
        this.asto.save(read, new Content.From("Hello".getBytes(charset))).join();
        new RxStorageValuePipeline<>(this.asto, read, write).process(
            (input, out) -> {
                try {
                    IOUtils.write(
                        String.join(" ", IOUtils.toString(input.get(), charset), "world!"),
                        out, charset
                    );
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                }
            }
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Storage item to read stays intact",
            new String(new BlockingStorage(this.asto).value(read), charset),
            new IsEqual<>("Hello")
        );
        MatcherAssert.assertThat(
            "Data were written to `write` location",
            new String(new BlockingStorage(this.asto).value(write), charset),
            new IsEqual<>("Hello world!")
        );
    }
}
