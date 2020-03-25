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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Supported algorithms for hashing.
 *
 * @since 0.3.3
 */
public enum Digest {
    /**
     * Supported algorithm enumeration: SHA-1, SHA-256.
     */
    SHA1("SHA-1", "sha"), SHA256("SHA-256", "sha256");

    /**
     * Algorithm used to instantiate MessageDigest instance.
     */
    private final String hashalg;

    /**
     * Algorithm name used in RPM metadata as checksum type.
     */
    private final String type;

    /**
     * Ctor.
     * @param alg Hashing algorithm
     * @param type Short name of the algorithm used in RPM metadata.
     */
    Digest(final String alg, final String type) {
        this.hashalg = alg;
        this.type = type;
    }

    /**
     * Instantiate MessageDigest instance.
     * @return MessageDigest instance
     */
    public MessageDigest messageDigest() {
        try {
            return MessageDigest.getInstance(this.hashalg);
        } catch (final NoSuchAlgorithmException err) {
            throw new IllegalStateException(
                String.format(
                    "%s is unavailable on this environment",
                    this.hashalg
                ),
                err
            );
        }
    }

    /**
     * Returns short algorithm name for using in RPM metadata.
     * @return Digest type
     */
    public String type() {
        return this.type;
    }
}
