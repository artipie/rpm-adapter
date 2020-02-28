package com.artipie.rpm;

import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
     * Add SHA1 prefix to names.
     * @since 0.3
     */
    final class Sha1Prefixed implements NamingPolicy {

        /**
         * SHA1 message digest supplier.
         */
        private final Supplier<MessageDigest> dgst;

        /**
         * Default ctor.
         */
        public Sha1Prefixed() {
            this(Sha1Prefixed::sha1Digest);
        }

        /**
         * Primary ctor.
         * @param dgst SHA1 digest
         */
        private Sha1Prefixed(final Supplier<MessageDigest> dgst) {
            this.dgst = dgst;
        }

        @Override
        public CompletionStage<String> name(
            final String source, final Publisher<ByteBuffer> content
        ) {
            return Flowable.fromPublisher(content)
                .reduce(this.dgst.get(), (acc, buf) -> {
                    acc.update(buf);
                    return acc;
                }).map(res -> new HexOf(new BytesOf(res.digest())).asString())
                .map(hex -> String.format("%s-%s", hex, source))
                .to(SingleInterop.get());
        }

        /**
         * SHA1 message digest.
         * @return Digest
         */
        private static MessageDigest sha1Digest() {
            try {
                return MessageDigest.getInstance("SHA1");
            } catch (final NoSuchAlgorithmException err) {
                throw new IllegalStateException(
                    "SHA1 is unavailable on this environment", err
                );
            }
        }
    }
}
