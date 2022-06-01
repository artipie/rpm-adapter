/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentAs;
import com.artipie.asto.misc.UncheckedIOConsumer;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * For test.
 *
 * @param <R> sssssddddd
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public class SyncRpmStorageValuePipeline<R> {

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Storage item key to read from.
     */
    private final Key read;

    /**
     * Storage item key to write to.
     */
    private final Key write;

    /**
     * Ctor.
     *
     * @param asto Abstract storage
     * @param read Storage item key to read from
     * @param write Storage item key to write to
     */
    public SyncRpmStorageValuePipeline(final Storage asto, final Key read, final Key write) {
        this.asto = asto;
        this.read = read;
        this.write = write;
    }

    /**
     * Ctor.
     *
     * @param asto Abstract storage
     * @param key Item key
     */
    public SyncRpmStorageValuePipeline(final Storage asto, final Key key) {
        this(asto, key, key);
    }

    /**
     * Process storage item and save it back.
     *
     * @param action Action output stream.
     * @return Completion action
     * @throws ArtipieIOException On Error
     */
    public CompletionStage<Void> process(
        final BiConsumer<Optional<InputStream>, OutputStream> action
    ) {
        return this.processWithResult(
            (opt, input) -> {
                action.accept(opt, input);
                return null;
            }
        ).thenAccept(
            nothing -> {
            }
        );
    }

    /**
     * Process storage item, save it back and return some result.
     *
     * @param action Action output stream.
     * @return Completion action with the result
     * @throws ArtipieIOException On Error
     */
    public CompletionStage<R> processWithResult(
        final BiFunction<Optional<InputStream>, OutputStream, R> action
    ) {
        final AtomicReference<R> res = new AtomicReference<>();
        final CompletionStage<Optional<InputStream>> fut = this.asto.exists(this.read)
            .thenCompose(
                exists -> {
                    final CompletionStage<Optional<InputStream>> stage;
                    if (exists) {
                        stage = this.asto.value(this.read)
                            .thenCompose(
                                content -> ContentAs.BYTES.apply(
                                    Single.just(
                                        content
                                    )
                                ).to(SingleInterop.get())
                            ).thenApply(
                                bytes -> Optional.of(new ByteArrayInputStream(bytes))
                            );
                    } else {
                        stage = CompletableFuture.completedFuture(Optional.empty());
                    }
                    return stage;
                }
            );
        return fut
            .thenApply(
                optional -> {
                    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                        res.set(action.apply(optional, output));
                        return new Content.From(output.toByteArray());
                    } catch (final IOException err) {
                        throw new ArtipieIOException(err);
                    } finally {
                        optional.ifPresent(new UncheckedIOConsumer<>(InputStream::close));
                    }
                }
            ).thenCompose(content -> this.asto.save(this.write, content))
            .thenApply(nothing -> res.get());
    }
}
