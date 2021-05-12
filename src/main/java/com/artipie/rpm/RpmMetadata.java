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

import com.artipie.rpm.meta.XmlAlter;
import com.artipie.rpm.meta.XmlMaid;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlPrimaryMaid;
import com.artipie.rpm.pkg.FilePackage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Rpm metadata class works with xml metadata - adds or removes records about xml packages.
 * @since 1.4
 */
public interface RpmMetadata {

    /**
     * Removes RMP records from metadata.
     * @since 1.4
     */
    final class Remove {

        /**
         * Metadata list.
         */
        private final Collection<MetadataItem> items;

        /**
         * Ctor.
         * @param items Metadata items
         */
        public Remove(final MetadataItem... items) {
            this.items = Arrays.asList(items);
        }

        /**
         * Removes records from metadata by RPMs checksums.
         * @param checksums Rpms checksums  to remove by
         * @throws IOException On io-operation result error
         */
        public void perform(final Collection<String> checksums) throws IOException {
            for (final MetadataItem item : this.items) {
                final Path temp = Files.createTempFile("rpm-index", "xml");
                try {
                    final long res;
                    final XmlMaid maid;
                    try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(temp))) {
                        if (item.type == XmlPackage.PRIMARY) {
                            maid = new XmlPrimaryMaid.Stream(item.input, out);
                        } else {
                            maid = new XmlMaid.ByPkgidAttr.Stream(item.input, out);
                        }
                        res = maid.clean(checksums);
                    }
                    try (InputStream input = new BufferedInputStream(Files.newInputStream(temp))) {
                        new XmlAlter.Stream(input, item.out)
                            .pkgAttr(item.type.tag(), String.valueOf(res));
                    }
                } finally {
                    Files.delete(temp);
                }
            }
        }
    }

    /**
     * Appends RMP records into metadata.
     * @since 1.4
     */
    final class Append {

        /**
         * Metadata list.
         */
        private final Collection<MetadataItem> items;

        /**
         * Ctor.
         * @param items Metadata items
         */
        public Append(final MetadataItem... items) {
            this.items = Arrays.asList(items);
        }

        /**
         * Appends records about provided RPMs.
         * @param packages Rpms to append info about
         */
        public void perform(final Collection<FilePackage> packages) {
            throw new NotImplementedException("Not implemented yet");
        }
    }

    /**
     * Metadata item.
     * @since 1.4
     */
    final class MetadataItem {

        /**
         * Xml metadata type.
         */
        private final XmlPackage type;

        /**
         * Xml metadata input stream.
         */
        private final InputStream input;

        /**
         * Xml metadata output, where write the result.
         */
        private final OutputStream out;

        /**
         * Ctor.
         * @param type Xml type
         * @param input Xml metadata input stream
         * @param out Xml metadata output, where write the result
         */
        public MetadataItem(final XmlPackage type, final InputStream input,
            final OutputStream out) {
            this.type = type;
            this.input = input;
            this.out = out;
        }
    }
}
