/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm;

import java.io.IOException;
import java.nio.file.Path;

/**
 * RPM repository metadata files naming policy.
 * @since 0.3
 */
public interface NamingPolicy {

    /**
     * Name for source with its content.
     * @param source Metadata file name
     * @param content Metadata file content
     * @return File name
     * @throws IOException On error
     */
    String name(String source, Path content) throws IOException;

    /**
     * Add hash prefix to names.
     * @since 0.3
     */
    final class HashPrefixed implements NamingPolicy {

        /**
         * Message digest supplier.
         */
        private final Digest dgst;

        /**
         * Ctor.
         * @param dgst One of the supported digest algorithms
         */
        public HashPrefixed(final Digest dgst) {
            this.dgst = dgst;
        }

        @Override
        public String name(final String source, final Path content) throws IOException {
            return String.format("%s-%s", new FileChecksum(content, this.dgst).hex(), source);
        }
    }
}
