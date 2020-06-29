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
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.cactoos.list.ListOf;

/**
 * Test rpm.
 * @since 0.9
 */
public interface TestRpm {

    /**
     * Puts test package to storage.
     * @param storage Storage
     * @throws IOException On error
     */
    void put(Storage storage) throws IOException;

    /**
     * Name of the rpm.
     * @return Name of the rpm
     */
    String name();

    /**
     * Time sentos rpm.
     * @since 0.9
     */
    final class Time extends FromPath {

        /**
         * Ctor.
         */
        public Time() {
            super("time-1.7-45.el7.x86_64.rpm");
        }
    }

    /**
     * Abc test rpm.
     * @since 0.9
     */
    final class Abc extends FromPath {

        /**
         * Ctor.
         */
        public Abc() {
            super("abc-1.01-26.git20200127.fc32.ppc64le.rpm");
        }
    }

    /**
     * Libdeflt test rpm.
     * @since 0.9
     */
    final class Libdeflt extends FromPath {

        /**
         * Ctor.
         */
        public Libdeflt() {
            super("libdeflt1_0-2020.03.27-25.1.armv7hl.rpm");
        }
    }

    /**
     * Abstract from file implementation.
     * @since 0.9
     */
    abstract class FromPath implements TestRpm {

        /**
         * Test resources dir.
         */
        private static final Path RESOURCES = Paths.get("src/test/resources-binary/");

        /**
         * Origin.
         */
        private final Path path;

        /**
         * Ctor.
         * @param file Rpm file name
         */
        protected FromPath(final String file) {
            this(RESOURCES.resolve(file));
        }

        /**
         * Primary ctor.
         * @param path Rpm file path
         */
        private FromPath(final Path path) {
            this.path = path;
        }

        @Override
        public final void put(final Storage storage) throws IOException {
            storage.save(
                new Key.From(this.path.getFileName().toString()),
                new Content.From(Files.readAllBytes(this.path))
            ).join();
        }

        @Override
        public String name() {
            return this.path.getFileName().toString().replaceAll("\\.rpm$", "");
        }
    }

    /**
     * An invalid rpm.
     * @since 0.9
     */
    final class Invalid implements TestRpm {
        @Override
        public void put(final Storage storage) throws IOException {
            storage.save(
                new Key.From(String.format("%s.rpm", this.name())),
                new Content.From(new byte[] {0x00, 0x01, 0x02 })
            ).join();
        }

        @Override
        public String name() {
            return "invalid";
        }

    }

    /**
     * Multiple test rpms.
     * @since 0.9
     */
    final class Multiple implements TestRpm {

        /**
         * Rpms.
         */
        private final Iterable<TestRpm> rpms;

        /**
         * Ctor.
         * @param rpms Rpms.
         */
        public Multiple(final TestRpm... rpms) {
            this(new ListOf<>(rpms));
        }

        /**
         * Ctor.
         * @param rpms Rpms.
         */
        public Multiple(final Iterable<TestRpm> rpms) {
            this.rpms = rpms;
        }

        @Override
        public void put(final Storage storage) throws IOException {
            for (final TestRpm rpm: this.rpms) {
                rpm.put(storage);
            }
        }

        @Override
        public String name() {
            throw new UnsupportedOperationException();
        }
    }
}
