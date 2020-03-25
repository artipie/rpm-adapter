/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
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

import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import org.cactoos.io.BytesOf;
import org.cactoos.text.HexOf;
import org.reactivestreams.Publisher;

/**
 * RPM files naming policy.
 * @since 0.3
 */
public interface NamingPolicy {

    /**
     * Default naming policy, just returns source name.
     */
    NamingPolicy DEFAULT = (src, cnt) -> CompletableFuture.completedFuture(src);

    /**
     * Name for source with its content.
     * @param source RPM file
     * @param content RPM file content
     * @return Async file name
     */
    CompletionStage<String> name(String source, Publisher<ByteBuffer> content);

    /**
     * Add hash prefix to names. Uses SHA256 by default
     * @since 0.3
     */
    final class HashPrefixed implements NamingPolicy {

        /**
         * Message digest supplier.
         */
        private final Supplier<MessageDigest> dgst;

        /**
         * Ctor.
         * @param dgst One of the supported digest algorithms
         */
        public HashPrefixed(final Digest dgst) {
            this(dgst::messageDigest);
        }

        /**
         * Primary ctor.
         * @param dgst Message digest supplier
         */
        private HashPrefixed(final Supplier<MessageDigest> dgst) {
            this.dgst = dgst;
        }

        @Override
        public CompletionStage<String> name(
            final String source, final Publisher<ByteBuffer> content
        ) {
            return Flowable.fromPublisher(content)
                .reduce(
                    this.dgst.get(),
                    (acc, buf) -> {
                        acc.update(buf);
                        return acc;
                    }
                )
                .map(res -> new HexOf(new BytesOf(res.digest())).asString())
                .map(hex -> String.format("%s-%s", hex, source))
                .to(SingleInterop.get());
        }
    }
}
