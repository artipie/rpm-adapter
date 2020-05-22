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
package com.artipie.rpm.meta;

import com.artipie.rpm.pkg.FilelistsOutput;
import com.artipie.rpm.pkg.OthersOutput;
import com.artipie.rpm.pkg.PackageOutput;
import com.artipie.rpm.pkg.PrimaryOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;

/**
 * Xml metadata packages.
 * @since 0.9
 */
public enum XmlPackage {

    /**
     * Metadata primary.xml.
     */
    PRIMARY(
        "metadata",
        new MapOf<String, String>(
            new MapEntry<>("", "http://linux.duke.edu/metadata/common"),
            new MapEntry<>("rpm", "http://linux.duke.edu/metadata/rpm")
        )
    ) {
        @Override
        public PackageOutput.FileOutput output() throws IOException {
            return new PrimaryOutput(Files.createTempFile(this.tempPrefix(), XmlPackage.SFX));
        }
    },

    /**
     * Metadata other.xml.
     */
    OTHER(
        "otherdata",
        new MapOf<String, String>(new MapEntry<>("", "http://linux.duke.edu/metadata/other"))
    ) {
        @Override
        public PackageOutput.FileOutput output() throws IOException {
            return new OthersOutput(Files.createTempFile(this.tempPrefix(), XmlPackage.SFX));
        }
    },

    /**
     * Metadata filelists.xml.
     */
    FILELISTS(
        "filelists",
        new MapOf<String, String>(new MapEntry<>("", "http://linux.duke.edu/metadata/filelists"))
    ) {
        @Override
        public PackageOutput.FileOutput output() throws IOException {
            return new FilelistsOutput(Files.createTempFile(this.tempPrefix(), XmlPackage.SFX));
        }
    };

    /**
     * File suffix.
     * @checkstyle ConstantUsageCheck (5 lines)
     */
    private static final String SFX = ".xml";

    /**
     * Tag name.
     */
    private final String tagname;

    /**
     * Metadata namespaces.
     */
    private final Map<String, String> namespaces;

    /**
     * Ctor.
     * @param tagname Tag name
     * @param namespaces Namespaces
     */
    XmlPackage(final String tagname, final Map<String, String> namespaces) {
        this.tagname = tagname;
        this.namespaces = namespaces;
    }

    /**
     * Metadata tag.
     * @return String tag
     */
    public String tag() {
        return this.tagname;
    }

    /**
     * Metadata file name.
     * @return String file name.
     */
    public String filename() {
        return this.name().toLowerCase(Locale.getDefault());
    }

    /**
     * Metadata file prefix name.
     * @return String file prefix name.
     */
    public String tempPrefix() {
        return String.format("%s-", this.filename());
    }

    /**
     * Returns xml namespaces.
     * @return Map of the namespaces.
     */
    public Map<String, String> xmlNamespaces() {
        return this.namespaces;
    }

    /**
     * File output for the xml package.
     * @return FileOutput instance
     * @throws IOException On error
     */
    public abstract PackageOutput.FileOutput output() throws IOException;

    /**
     * List of XmlPackage.
     * @since 0.10
     */
    public static final class Stream {

        /**
         * Need filelists?
         */
        private final boolean filelists;

        /**
         * Ctor.
         * @param filelists Need fileslist?
         */
        public Stream(final boolean filelists) {
            this.filelists = filelists;
        }

        /**
         * Stream of XmlPackage values.
         * @return Stream of XmlPackage
         */
        public java.util.stream.Stream<XmlPackage> get() {
            final java.util.stream.Stream<XmlPackage> res;
            if (this.filelists) {
                res = java.util.stream.Stream.of(values());
            } else {
                res = java.util.stream.Stream.of(PRIMARY, OTHER);
            }
            return res;
        }
    }

}
