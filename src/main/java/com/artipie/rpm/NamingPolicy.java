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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.function.Supplier;
import org.bouncycastle.util.encoders.Hex;

/**
 * RPM files naming policy.
 * @since 0.3
 */
public interface NamingPolicy {

    /**
     * Name for source with its content.
     * @param source RPM file
     * @param content RPM file content
     * @return File name
     * @throws IOException On error
     */
    String name(String source, Path content) throws IOException;

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
        public String name(final String source, final Path content) throws IOException {
            final MessageDigest digest = this.dgst.get();
            final ByteBuffer buffer = ByteBuffer.allocate(1024 * 8);
            try (FileChannel chan = FileChannel.open(content, StandardOpenOption.READ)) {
                while (chan.read(buffer) > 0) {
                    digest.update(buffer);
                }
            }
            return String.format("%s-%s", Hex.toHexString(digest.digest()), source);
        }
    }
}
