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
package com.artipie.rpm.files;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cactoos.Scalar;
import org.cactoos.io.OutputStreamTo;
import org.cactoos.io.TeeInputStream;

/**
 * Downloads test bundle into temp dir.
 * @since 0.8
 * @todo #120:30min Create test for this class and use it in RpmITCase to download and
 *  UnpackGzip to unpack rmps from tar.gz. Use longTests profile for mentioned tests.
 */
public final class DownloadBundle implements Scalar<Path> {

    /**
     * Size of the bundle.
     */
    private final URL url;

    /**
     * Ctor.
     * @param url URL
     */
    public DownloadBundle(final URL url) {
        this.url = url;
    }

    /**
     * Ctor.
     * @param size Bundle size
     */
    public DownloadBundle(final Size size) {
        this(size.url());
    }

    @Override
    public Path value() throws IOException {
        final String[] parts = this.url.getPath().split("/");
        final String name = parts[parts.length - 1];
        final File gzip = Files.createTempDirectory("downloads").resolve(name).toFile();
        gzip.createNewFile();
        try (TeeInputStream tee =
            new TeeInputStream(this.url.openStream(), new OutputStreamTo(gzip))) {
            tee.read();
        }
        return gzip.toPath();
    }

    /**
     * Bundle size.
     */
    enum Size {

        /**
         * Hundred rpms bundle.
         */
        HUNDRED("https://artipie.s3.amazonaws.com/rpm-test/bundle100.tar.gz"),

        /**
         * Hundred rpms bundle.
         */
        THOUSAND("https://artipie.s3.amazonaws.com/rpm-test/bundle1000.tar.gz");

        /**
         * Value.
         */
        private final String val;

        /**
         * Ctor.
         * @param val Value
         */
        Size(final String val) {
            this.val = val;
        }

        /**
         * Returns ULR instance.
         * @return Url
         */
        URL url() {
            try {
                return new URL(this.val);
            } catch (final MalformedURLException ex) {
                throw new IllegalArgumentException("Invalid url", ex);
            }
        }

    }
}
