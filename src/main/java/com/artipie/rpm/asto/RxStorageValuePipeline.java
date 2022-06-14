/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.ByteArray;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.misc.UncheckedIOConsumer;
import com.artipie.asto.misc.UncheckedIOSupplier;
import io.reactivex.Flowable;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DefaultSubscriber;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.reactivestreams.Publisher;

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
 * @checkstyle JavadocLocationCheck (1000 lines)
 * @checkstyle ParameterNameCheck (1000 lines)
 * @checkstyle JavadocTagsCheck (1000 lines)
 * @checkstyle AbbreviationAsWordInNameCheck (1000 lines)
 * @checkstyle LineLengthCheck (1000 lines)
 * @checkstyle BracketsStructureCheck (1000 lines)
 */
public class RxStorageValuePipeline<R> {
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
    public RxStorageValuePipeline(final Storage asto, final Key read, final Key write) {
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
    public RxStorageValuePipeline(final Storage asto, final Key key) {
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
                        stage = this.asto.value(this.read)
                            .thenApply(
                                content -> {
                                    final InputStreamSubscriber subscriber = new InputStreamSubscriber();
                                    final InputStream input = subscriber.inputStream();
                                    Flowable.fromPublisher(content)
                                        .subscribeOn(Schedulers.io())
                                        .subscribe(subscriber);
                                    return Optional.of(input);
                                }
                            );
                    } else {
                        stage = CompletableFuture.completedFuture(Optional.empty());
                    }
                    return stage;
                }
            ).thenCompose(
                optional -> {
                    try (PublishingOutputStream output = new PublishingOutputStream()) {
                        res.set(action.apply(optional, output));
                        return this.asto.save(this.write, new Content.From(output.publisher()));
                    } catch (final IOException err) {
                        throw new ArtipieIOException(err);
                    } finally {
                        optional.ifPresent(new UncheckedIOConsumer<>(InputStream::close));
                    }
                }
            ).thenApply(nothing -> res.get());
    }

    /**
     * Hello!
     */
    private static class InputStreamSubscriber extends DefaultSubscriber<ByteBuffer> {

        private final PipedOutputStream out;
        private final PipedInputStream input;
        private final WritableByteChannel channel;

        InputStreamSubscriber() {
            this.out = new PipedOutputStream();
            this.input = new UncheckedIOSupplier<>(
                () -> new PipedInputStream(this.out)
            ).get();
            this.channel = Channels.newChannel(this.out);
        }

        @Override
        public void onNext(final ByteBuffer buffer) {
            new UncheckedIORunnable(() -> {
                while (buffer.hasRemaining()) {
                    this.channel.write(buffer);
                }
            }).run();
        }

        @Override
        public void onError(final Throwable err) {
            new UncheckedIORunnable(this.input::close).run();
        }

        @Override
        public void onComplete() {
            new UncheckedIORunnable(this.out::close).run();
        }

        InputStream inputStream() {
            return this.input;
        }
    }

    /**
     * Hello!
     */
    private static class PublishingOutputStream extends OutputStream {

        private final UnicastProcessor<ByteBuffer> pub;
        private final UnicastProcessor<Byte> proc;

        PublishingOutputStream() {
            this(100, TimeUnit.MILLISECONDS, 4 * 1024);
        }

        /**
         * Ctor.
         *
         * @param timespan The period of time buffer collects bytes before it is emitted to publisher.
         * @param unit The unit of time which applies to the timespan argument.
         * @param count The maximum size of each buffer before it is emitted.
         */
        @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
        PublishingOutputStream(
            final long timespan,
            final TimeUnit unit,
            final int count
        ) {
            this.pub = UnicastProcessor.create();
            this.proc = UnicastProcessor.create();
            this.proc.buffer(timespan, unit, count)
                .doOnNext(
                    list -> this.pub.onNext(
                        ByteBuffer.wrap(new ByteArray(list).primitiveBytes())
                    )
                )
                .subscribeOn(Schedulers.io())
                .doOnComplete(this.pub::onComplete)
                .subscribe();
        }

        @Override
        public void write(final int b) throws IOException {
            this.proc.onNext((byte) b);
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.proc.onComplete();
        }

        Publisher<ByteBuffer> publisher() {
            return this.pub;
        }
    }

    /**
     * Hello!
     */
    private static class UncheckedIORunnable {

        private final IORunnable runnable;

        UncheckedIORunnable(final IORunnable runnable) {
            this.runnable = runnable;
        }

        void run() {
            try {
                this.runnable.run();
            } catch (final IOException err) {
                throw new ArtipieIOException(err);
            }
        }
    }

    /**
     * Hello!
     */
    @FunctionalInterface
    private interface IORunnable {
        void run() throws IOException;
    }
}
