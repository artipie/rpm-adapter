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

import io.reactivex.Single;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.xml.bind.DatatypeConverter;

/**
 * Hashing sum of a file.
 *
 * @since 0.1
 */
final class Checksum {

    /**
     * The XML.
     */
    private final Path file;

    /**
     * Message digest.
     */
    private final Digest dgst;

    /**
     * Ctor.
     * @param path The path
     * @param dgst The hashing algorithm for checksum computation.
     */
    Checksum(final Path path, final Digest dgst) {
        this.file = path;
        this.dgst = dgst;
    }

    /**
     * Calculate it.
     * @return The hash of the file content or error.
     */
    public Single<String> hash() {
        return Single.fromCallable(
            () -> Files.readAllBytes(this.file)
        ).map(
            bytes -> DatatypeConverter.printHexBinary(
                this.dgst.messageDigest().digest(bytes)
            ).toLowerCase(Locale.ENGLISH));
    }

}
