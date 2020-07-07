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
                    this.dir, String.format("old-%s", this.type.filename()), ".xml"
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
                        .contains(String.format("%s.xml.gz", this.type.filename()))
                ).findFirst();
            }
        }
    }

}
