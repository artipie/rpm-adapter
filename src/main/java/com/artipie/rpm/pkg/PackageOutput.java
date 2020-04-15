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
package com.artipie.rpm.pkg;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * RPM package output.
 * @since 0.6
 */
public interface PackageOutput extends Closeable {

    /**
     * Accept package metadata.
     * @param meta Metadata
     * @throws IOException On error
     */
    void accept(Package.Meta meta) throws IOException;

    /**
     * File output implementation.
     * @since 0.6
     */
    interface FileOutput extends PackageOutput {

        /**
         * File path.
         * @return Path
         */
        Path file();

        /**
         * Fake {@link FileOutput}.
         *
         * @since 1.0
         */
        final class Fake implements FileOutput {

            /**
             * File path.
             */
            private final Path file;

            /**
             * Ctor.
             *
             * @param file File path
             */
            public Fake(final Path file) {
                this.file = file;
            }

            /**
             * Start packages.
             * @return Self
             * @throws IOException On failure
             */
            public Fake start() throws IOException {
                Files.write(
                    this.file,
                    Arrays.asList("content")
                );
                return this;
            }

            @Override
            public void accept(final Package.Meta meta) {
                // nothing
            }

            @Override
            public void close() {
                // nothing
            }

            @Override
            public Path file() {
                return this.file;
            }
        }
    }

    /**
     * Multiple outputs.
     * @since 0.6
     */
    final class Multiple implements PackageOutput {

        /**
         * List of outputs.
         */
        private final Iterable<? extends PackageOutput> list;

        /**
         * Ctor.
         * @param outs Outputs
         */
        public Multiple(final PackageOutput... outs) {
            this(Arrays.asList(outs));
        }

        /**
         * Ctor.
         * @param outs Outputs
         */
        public Multiple(final Iterable<? extends PackageOutput> outs) {
            this.list = outs;
        }

        @Override
        public void accept(final Package.Meta meta) throws IOException {
            for (final PackageOutput out : this.list) {
                out.accept(meta);
            }
        }

        @Override
        public void close() throws IOException {
            final List<IOException> errors = new LinkedList<>();
            for (final PackageOutput out : this.list) {
                try {
                    out.close();
                } catch (final IOException err) {
                    errors.add(err);
                }
            }
            if (!errors.isEmpty()) {
                final IOException exc = new IOException("Couldn't close underlying outputs");
                errors.forEach(exc::addSuppressed);
                throw exc;
            }
        }
    }
}
