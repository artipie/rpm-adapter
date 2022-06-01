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
import io.reactivex.Single;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * For test.
 *
 * @param <R> sssssddddd
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.9
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
        return this.asto.exists(this.read)
            .thenCompose(
                exists -> {
                    try {
                        final Optional<InputStream> inpfrom;
                        if (exists) {
                            inpfrom = Optional.of(
                                new ByteArrayInputStream(
                                    ContentAs.BYTES.apply(
                                        Single.just(
                                            this.asto.value(this.read).join()
                                        )
                                    ).toFuture().get()
                                )
                            );
                        } else {
                            inpfrom = Optional.empty();
                        }
                        final ByteArrayOutputStream outto = new ByteArrayOutputStream();
                        final R res = action.apply(inpfrom, outto);
                        return this.asto.save(
                            this.write,
                            new Content.From(
                                outto.toByteArray()
                            )
                        ).thenApply(noting -> res);
                    } catch (final ExecutionException err) {
                        throw new ArtipieIOException(err);
                    } catch (final InterruptedException err) {
                        throw new RuntimeException(err);
                    }
                }
            );
    }

    /**
     * Future's handler to close streams.
     *
     * @param <T> Result type.
     * @since 1.12.0
     */
    private static class FutureHandler<T> implements BiFunction<T, Throwable, T> {
        /**
         * Streams to close.
         */
        private final Closeable[] streams;

        /**
         * Ctor.
         *
         * @param streams Streams to close.
         */
        FutureHandler(final Closeable... streams) {
            this.streams = Arrays.copyOf(streams, streams.length);
        }

        @Override
        public T apply(final T res, final Throwable err) {
            try {
                for (final Closeable stream : this.streams) {
                    stream.close();
                }
            } catch (final IOException ioe) {
                throw new ArtipieIOException(ioe);
            }
            if (err != null) {
                throw new ArtipieIOException(err);
            }
            return res;
        }
    }
}
