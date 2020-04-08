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
package com.artipie.rpm.pkg;

import com.artipie.rpm.Digest;
import com.artipie.rpm.FileChecksum;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.cactoos.Scalar;
import org.cactoos.list.ListOf;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Unchecked;
import org.redline_rpm.ReadableChannelWrapper;
import org.redline_rpm.Scanner;
import org.redline_rpm.header.AbstractHeader;
import org.redline_rpm.header.Format;
import org.redline_rpm.header.Header;

/**
 * Single package in a file.
 *
 * @since 0.1
 */
public final class FilePackage implements Package {

    /**
     * The RPM file.
     */
    private final Path file;

    /**
     * The header.
     */
    private final Unchecked<Header> hdr;

    /**
     * Ctor.
     * @param path The path
     */
    public FilePackage(final Path path) {
        this.file = path;
        this.hdr = new Unchecked<>(
            new Sticky<>(
                new Scalar<Header>() {
                    @Override
                    public Header value() throws Exception {
                        try (InputStream fios = Files.newInputStream(FilePackage.this.file)) {
                            final Format format = new Scanner(
                                new PrintStream(Logger.stream(Level.INFO, this))
                            ).run(new ReadableChannelWrapper(Channels.newChannel(fios)));
                            final Header header = format.getHeader();
                            Logger.debug(this, "header: %s", header.toString());
                            return header;
                        }
                    }
                }
            )
        );
    }

    /**
     * Get path.
     * @return Path
     */
    public Path path() {
        return this.file;
    }

    @Override
    public void save(final PackageOutput out, final Digest digest) throws IOException {
        out.accept(new FilePackage.Headers(this.header(), this.file, digest));
    }

    /**
     * Get header.
     * @return The header
     */
    private Header header() {
        return this.hdr.value();
    }

    /**
     * File package metadata.
     * @since 0.8
     * @todo #69:30min Create unit tests to verify that
     *  FilePackage headers can parse headers and header range correctly,
     *  can calculate file size and check-sums correctly.
     */
    private static final class Headers implements Meta {

        /**
         * Native headers.
         */
        private final Header hdr;

        /**
         * File path.
         */
        private final Path file;

        /**
         * Digest.
         */
        private final Digest digest;

        /**
         * Ctor.
         * @param hdr Native headers
         * @param file File path
         * @param digest Digest
         */
        Headers(final Header hdr, final Path file, final Digest digest) {
            this.hdr = hdr;
            this.file = file;
            this.digest = digest;
        }

        @Override
        public String header(final Header.HeaderTag tag, final String def) {
            final AbstractHeader.Entry<?> entry = this.hdr.getEntry(tag);
            final String val;
            if (entry == null) {
                val = "";
            } else {
                val = ((String[]) entry.getValues())[0];
            }
            return val;
        }

        @Override
        public int header(final Header.HeaderTag tag, final int def) {
            final AbstractHeader.Entry<?> entry = this.hdr.getEntry(tag);
            final int val;
            if (entry == null) {
                val = def;
            } else {
                val = ((int[]) entry.getValues())[0];
            }
            return val;
        }

        @Override
        public List<String> headers(final Header.HeaderTag tag) {
            return new ListOf<>((String[]) this.hdr.getEntry(tag).getValues());
        }

        @Override
        public int[] intHeaders(final Header.HeaderTag tag) {
            return (int[]) this.hdr.getEntry(tag).getValues();
        }

        @Override
        public Checksum checksum() {
            return new FileChecksum(this.file, this.digest);
        }

        @Override
        public long size() throws IOException {
            return Files.size(this.file);
        }

        @Override
        public String href() {
            return this.file.getFileName().toString();
        }

        @Override
        public int[] range() {
            return new int[]{
                this.hdr.getStartPos(),
                this.hdr.getEndPos(),
            };
        }
    }
}
