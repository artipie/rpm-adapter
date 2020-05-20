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

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;

/**
 * XML {@code filelists.xml} metadata file imperative writer.
 * <p>
 * This object is not thread safe and depends on order of method calls.
 * </p>
 * @since 0.4
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class XmlFilelists implements Closeable {

    /**
     * Xml file.
     */
    private final XmlFile xml;

    /**
     * XmlPackagesFile writer.
     */
    private final XmlPackagesFile packages;

    /**
     * Ctor.
     * @param file Path to write filelists.xml
     */
    public XmlFilelists(final Path file) {
        this(new XmlFile(file));
    }

    /**
     * Primary ctor.
     * @param xml XML file
     */
    public XmlFilelists(final XmlFile xml) {
        this.xml = xml;
        this.packages = new XmlPackagesFile(xml, XmlPackage.FILELISTS);
    }

    /**
     * Starts processing of RPMs.
     * @return Self
     * @throws XMLStreamException when XML generation causes error
     */
    public XmlFilelists startPackages() throws XMLStreamException {
        this.packages.startPackages();
        return this;
    }

    /**
     * Start new package.
     * @param name Package name
     * @param arch Package arch
     * @param checksum Package checksum
     * @return Package modifier
     * @throws XMLStreamException On XML error
     */
    public Package startPackage(final String name, final String arch, final String checksum)
        throws XMLStreamException {
        this.xml.writeStartElement("package");
        this.xml.writeAttribute("pkgid", checksum);
        this.xml.writeAttribute("name", name);
        this.xml.writeAttribute("arch", arch);
        return new Package(this, this.xml);
    }

    @Override
    public void close() throws IOException {
        this.packages.close();
    }

    /**
     * Package writer.
     * @since 0.6
     */
    public static final class Package {

        /**
         * Filelists reference.
         */
        private final XmlFilelists filelists;

        /**
         * XML stream.
         */
        private final XmlFile xml;

        /**
         * Ctor.
         * @param filelists Filelists
         * @param xml XML stream
         */
        Package(final XmlFilelists filelists, final XmlFile xml) {
            this.filelists = filelists;
            this.xml = xml;
        }

        /**
         * Add version.
         * @param epoch Epoch
         * @param ver Version
         * @param rel Release
         * @return Self
         * @throws XMLStreamException On XML error
         */
        public Package version(final int epoch, final String ver, final String rel)
            throws XMLStreamException {
            this.xml.writeEmptyElement("version");
            this.xml.writeAttribute("epoch", String.valueOf(epoch));
            this.xml.writeAttribute("ver", ver);
            this.xml.writeAttribute("rel", rel);
            return this;
        }

        /**
         * Add package files.
         * @param files Files
         * @param dirs Dirs
         * @param did Directory ids
         * @return Self
         * @throws XMLStreamException On failure
         */
        @SuppressWarnings("PMD.UseVarargs")
        public Package files(final String[] files, final String[] dirs, final int[] did)
            throws XMLStreamException {
            final Set<String> dirset = Arrays.stream(dirs).collect(Collectors.toSet());
            for (int idx = 0; idx < files.length; idx += 1) {
                final String fle = files[idx];
                if (fle.isEmpty() || fle.charAt(0) == '.') {
                    continue;
                }
                final String path = String.format("%s%s", dirs[did[idx]], fle);
                this.xml.writeStartElement("file");
                if (dirset.contains(String.format("%s/", path))) {
                    this.xml.writeAttribute("type", "dir");
                }
                this.xml.writeCharacters(path);
                this.xml.writeEndElement();
            }
            return this;
        }

        /**
         * Close a package.
         * @return Filelists
         * @throws XMLStreamException On error
         */
        public XmlFilelists close() throws XMLStreamException {
            this.xml.writeEndElement();
            return this.filelists;
        }
    }
}
