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
package com.artipie.rpm;

import com.artipie.asto.Content;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Single;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Subscriber;

/**
 * Content implementation for test resources.
 * @since 0.6
 */
public final class TestContent implements Content {

    /**
     * Default buffer 8K.
     */
    private static final int BUF_SIZE = 1024 * 8;

    /**
     * Resource name.
     */
    private final String name;

    /**
     * Content for test resource.
     * @param name Resource name
     */
    public TestContent(final String name) {
        this.name = name;
    }

    @Override
    public Optional<Long> size() {
        return Optional.empty();
    }

    @Override
    public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
        Single.just(Thread.currentThread().getContextClassLoader())
            .flatMapPublisher(
                ctx -> Flowable.create(
                    (FlowableOnSubscribe<ByteBuffer>) emitter -> {
                        final byte[] buf = new byte[TestContent.BUF_SIZE];
                        try (InputStream is = ctx.getResourceAsStream(this.name)) {
                            if (is == null) {
                                emitter.onError(
                                    new IllegalArgumentException(
                                        String.format("resource %s doesn't exist", this.name)
                                    )
                                );
                                return;
                            }
                            while (is.read(buf) > 0) {
                                emitter.onNext(ByteBuffer.wrap(buf));
                            }
                        } catch (final IOException err) {
                            emitter.onError(err);
                        }
                        emitter.onComplete();
                    },
                    BackpressureStrategy.BUFFER
                )
            ).subscribe(subscriber);
    }
}
