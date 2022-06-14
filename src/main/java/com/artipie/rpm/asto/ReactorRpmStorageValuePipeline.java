/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.ByteArray;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.misc.UncheckedIOConsumer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.Flowable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.netty.ByteBufFlux;

/**
 * For test.
 *
 * @param <R> sssssddddd
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (1000 lines)
 * @checkstyle JavadocMethodCheck (1000 lines)
 * @checkstyle DesignForExtensionCheck (1000 lines)
 * @checkstyle ConstructorOnlyInitializesOrCallOtherConstructors (1000 lines)
 * @checkstyle JavadocVariableCheck (1000 lines)
 * @checkstyle EmptyLineSeparatorCheck (1000 lines)
 */
public final class ReactorRpmStorageValuePipeline<R> {

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
    public ReactorRpmStorageValuePipeline(final Storage asto, final Key read, final Key write) {
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
    public ReactorRpmStorageValuePipeline(final Storage asto, final Key key) {
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
        return this.asto.exists(this.read)
            .thenCompose(
                exists -> {
                    final CompletionStage<Optional<InputStream>> stage;
                    if (exists) {
                        stage = Mono.fromCompletionStage(this.asto.value(this.read))
                            .map(
                                content -> (Publisher<ByteBuf>) Flowable.fromPublisher(content)
                                    .map(Unpooled::wrappedBuffer)
                            )
                            .map(
                                pub -> ByteBufFlux
                                    .fromInbound(Flux.from(pub))
                                    .asInputStream()
                            )
                            .flatMap((Function<Flux<InputStream>, Mono<InputStream>>) Flux::last)
                            .toFuture()
                            .thenApply(Optional::of);
                    } else {
                        stage = CompletableFuture.completedFuture(Optional.empty());
                    }
                    return stage;
                }
            ).thenApply(
                optional -> {
                    final Publisher<ByteBuf> publisher;
                    try (PublishingOutputStream output = new PublishingOutputStream()) {
                        res.set(action.apply(optional, output));
                        publisher = output.publisher();
                    } catch (final IOException err) {
                        throw new ArtipieIOException(err);
                    } finally {
                        optional.ifPresent(new UncheckedIOConsumer<>(InputStream::close));
                    }
                    return new Content.From(
                        Flux.from(publisher)
                            .map(ByteBuf::nioBuffer)
                    );
                }
            ).thenCompose(content -> this.asto.save(this.write, content))
            .thenApply(nothing -> res.get());
    }

    /**
     * Capital letter.
     *
     * @since 1.1.0
     */
    static final class PublishingOutputStream extends OutputStream {

        private final Sinks.Many<Byte> buffer;

        private final Sinks.Many<ByteBuf> pub;

        /**
         * Ctor.
         * @checkstyle ConstructorOnlyInitializesOrCallOtherConstructors (100 lines)
         */
        @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
        PublishingOutputStream() {
            this.buffer = Sinks.many().unicast().onBackpressureBuffer();
            this.pub = Sinks.many().unicast().onBackpressureBuffer();
            this.buffer.asFlux()
                .bufferTimeout(1024, Duration.ofMillis(50))
                .doOnNext(
                    byteList -> this.pub.tryEmitNext(
                        Unpooled.wrappedBuffer(new ByteArray(byteList).primitiveBytes())
                    )
                )
                .doOnComplete(this.pub::tryEmitComplete)
                .subscribeOn(Schedulers.newSingle("publisherThread"))
                .subscribe();
        }

        @Override
        // @checkstyle ParameterNameCheck (10 lines)
        public void write(final int b) throws IOException {
            this.buffer.tryEmitNext((byte) b);
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.buffer.tryEmitComplete();
        }

        Publisher<ByteBuf> publisher() {
            return this.pub.asFlux();
        }
    }

}
