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

import com.artipie.rpm.Digest;
import com.artipie.rpm.FileChecksum;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.redline_rpm.header.AbstractHeader;
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
     * Ctor.
     * @param path The path
     */
    public FilePackage(final Path path) {
        this.file = path;
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
        out.accept(
            new FilePackage.Headers(new FilePackageHeader(this.file).header(), this.file, digest)
        );
        Files.delete(this.file);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", this.getClass().getSimpleName(), this.file.getFileName());
    }

    /**
     * Parsed file package.
     * @return Parsed package
     * @throws InvalidPackageException In case package is invalid.
     * @throws IOException On error
     */
    public Package parsed() throws InvalidPackageException, IOException {
        return new ParsedFilePackage(new FilePackageHeader(this.file).header(), this.file);
    }

    /**
     * File package metadata.
     * @since 0.6
     */
    static final class Headers implements Meta {

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
        public MetaHeader header(final Header.HeaderTag tag) {
            return new EntryHeader(this.hdr.getEntry(tag));
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

    /**
     * {@link AbstractHeader.Entry} based MetaHeader.
     *
     * @since 0.6.3
     */
    private static final class EntryHeader implements MetaHeader {

        /**
         * Native header entry.
         */
        private final Optional<AbstractHeader.Entry<?>> entry;

        /**
         * Ctor.
         * @param entry Native header entry
         */
        EntryHeader(final AbstractHeader.Entry<?> entry) {
            this(Optional.ofNullable(entry));
        }

        /**
         * Ctor.
         * @param entry Native header entry
         */
        EntryHeader(final Optional<AbstractHeader.Entry<?>> entry) {
            this.entry = entry;
        }

        @Override
        public String asString(final String def) {
            return this.entry
                .map(e -> ((String[]) e.getValues())[0])
                .orElse(def);
        }

        @Override
        public int asInt(final int def) {
            return this.entry
                .map(e -> ((int[]) e.getValues())[0])
                .orElse(def);
        }

        @Override
        public List<String> asStrings() {
            return this.entry
                .map(e -> Arrays.asList((String[]) e.getValues()))
                .orElse(Collections.emptyList());
        }

        @Override
        public int[] asInts() {
            return this.entry
                .map(e -> (int[]) e.getValues())
                .orElseGet(() -> new int[0]);
        }
    }
}
