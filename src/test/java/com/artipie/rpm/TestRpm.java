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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test rpm.
 * @since 0.9
 * @todo #235:30min Continue test rpms implementation: first, create `Multiple` implementation of
 *  this interface. Multiple implementation should accept several TestRpm in the ctor and put all
 *  the rpms into storage on `put`. Second, create `Invalid` implementation to add invalid package
 *  to storage. Third, find all usages of the test rpms and replace with these classes usages.
 */
public interface TestRpm {

    /**
     * Puts test package to storage.
     * @param storage Storage
     * @throws IOException On error
     */
    void put(Storage storage) throws IOException;

    /**
     * Abc test rpm.
     * @since 0.9
     */
    final class Abc extends FromPath {

        /**
         * Ctor.
         */
        protected Abc() {
            super(Paths.get("src/test/resources-binary/abc-1.01-26.git20200127.fc32.ppc64le.rpm"));
        }
    }

    /**
     * Abc test rpm.
     * @since 0.9
     */
    final class Libdeflt extends FromPath {

        /**
         * Ctor.
         */
        protected Libdeflt() {
            super(Paths.get("src/test/resources-binary/libdeflt1_0-2020.03.27-25.1.armv7hl.rpm"));
        }
    }

    /**
     * Abstract from file implementation.
     * @since 0.9
     */
    abstract class FromPath implements TestRpm {

        /**
         * Origin.
         */
        private final Path path;

        /**
         * Ctor.
         * @param path Rpm file path
         */
        protected FromPath(final Path path) {
            this.path = path;
        }

        @Override
        public final void put(final Storage storage) throws IOException {
            try {
                new BlockingStorage(storage).save(
                    new Key.From(this.path.getFileName().toString()),
                    Files.readAllBytes(this.path)
                );
            } catch (final InterruptedException err) {
                Thread.currentThread().interrupt();
                throw new IOException("Failed to save", err);
            }
        }
    }
}
