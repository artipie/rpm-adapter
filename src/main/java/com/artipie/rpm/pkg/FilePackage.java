/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
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
     * File package metadata.
     * @since 0.6
     */
    public static final class Headers implements Meta {

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
         * The RPM file location relatively to the updated repository.
         */
        private final String location;

        /**
         * Ctor.
         * @param hdr Native headers
         * @param file File path
         * @param digest Digest
         * @param location File relative location
         * @checkstyle ParameterNumberCheck (10 lines)
         */
        public Headers(final Header hdr, final Path file, final Digest digest,
            final String location) {
            this.hdr = hdr;
            this.file = file;
            this.digest = digest;
            this.location = location;
        }

        /**
         * Ctor for tests.
         * @param hdr Native headers
         * @param file File path
         * @param digest Digest
         */
        public Headers(final Header hdr, final Path file, final Digest digest) {
            this(hdr, file, digest, file.getFileName().toString());
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
            return this.location;
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
    public static final class EntryHeader implements MetaHeader {

        /**
         * Native header entry.
         */
        private final Optional<AbstractHeader.Entry<?>> entry;

        /**
         * Ctor.
         * @param entry Native header entry
         */
        public EntryHeader(final AbstractHeader.Entry<?> entry) {
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
