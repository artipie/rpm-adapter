/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.rpm.files.Gzip;
import com.artipie.rpm.meta.XmlPackage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Metadata file from RPM repository being updated.
 * @since 0.11
 */
public interface PrecedingMetadata {

    /**
     * Does the metadata exists?
     * @return True is metadata exists
     * @throws IOException On error
     */
    boolean exists() throws IOException;

    /**
     * Path to unzipped metadata file if found.
     * @return Path
     * @throws IOException On error
     */
    Optional<Path> findAndUnzip() throws IOException;

    /**
     * From directory {@link PrecedingMetadata} implementation.
     * @since 0.11
     */
    final class FromDir implements PrecedingMetadata {

        /**
         * Xml package type.
         */
        private final XmlPackage type;

        /**
         * Directory path.
         */
        private final Path dir;

        /**
         * Ctor.
         * @param type Xml package type
         * @param dir Directory
         */
        public FromDir(final XmlPackage type, final Path dir) {
            this.type = type;
            this.dir = dir;
        }

        @Override
        public boolean exists() throws IOException {
            return this.find().isPresent();
        }

        @Override
        public Optional<Path> findAndUnzip() throws IOException {
            final Optional<Path> metadata = this.find();
            final Optional<Path> res;
            if (metadata.isPresent()) {
                final Path unziped = Files.createTempFile(
                    this.dir, String.format("old-%s", this.type.lowercase()), ".xml"
                );
                new Gzip(metadata.get()).unpack(unziped);
                res = Optional.of(unziped);
            } else {
                res = Optional.empty();
            }
            return res;
        }

        /**
         * Path to the metadata file if found.
         * @return Path
         * @throws IOException On error
         */
        private Optional<Path> find() throws IOException {
            try (Stream<Path> files = Files.walk(this.dir)) {
                return files.filter(
                    path -> path.getFileName().toString()
                        .contains(String.format("%s.xml.gz", this.type.lowercase()))
                ).findFirst();
            }
        }
    }

}
