/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.rpm.meta.XmlMaid;
import com.artipie.rpm.misc.UncheckedConsumer;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.StreamSupport;

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
         * Returns {@link XmlMaid} instance.
         * @return Xml maid
         */
        XmlMaid maid();

        /**
         * File tag.
         * @return String tag
         */
        String tag();

        /**
         * Start packages.
         * @return Self
         * @throws IOException On failure
         */
        FileOutput start() throws IOException;

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
             * Was package output accepted?
             */
            private boolean accepted;

            /**
             * Ctor.
             *
             * @param file File path
             */
            public Fake(final Path file) {
                this.file = file;
                this.accepted = false;
            }

            @Override
            public Fake start() throws IOException {
                Files.write(
                    this.file,
                    Arrays.asList("content")
                );
                return this;
            }

            @Override
            public void accept(final Package.Meta meta) {
                this.accepted = true;
            }

            @Override
            public void close() {
                // nothing
            }

            @Override
            public Path file() {
                return this.file;
            }

            @Override
            public XmlMaid maid() {
                return null;
            }

            @Override
            public String tag() {
                return "fake";
            }

            /**
             * Was package output accepted?
             * @return True if {@link Fake#accept(Package.Meta)} was called
             */
            public boolean isAccepted() {
                return this.accepted;
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
        private final Iterable<? extends Metadata> list;

        /**
         * Ctor.
         * @param outs Outputs
         */
        public Multiple(final Metadata... outs) {
            this(Arrays.asList(outs));
        }

        /**
         * Ctor.
         * @param outs Outputs
         */
        public Multiple(final Iterable<? extends Metadata> outs) {
            this.list = outs;
        }

        @Override
        public void accept(final Package.Meta meta) {
            StreamSupport.stream(this.list.spliterator(), true).forEach(
                new UncheckedConsumer<>(out -> out.accept(meta))
            );
        }

        @Override
        public void close() throws IOException {
            final List<IOException> errors = new LinkedList<>();
            for (final Metadata out : this.list) {
                try {
                    out.close();
                    out.brush(Collections.emptyList());
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
