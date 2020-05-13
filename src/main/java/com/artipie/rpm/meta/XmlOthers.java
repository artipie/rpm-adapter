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
import java.util.List;
import javax.xml.stream.XMLStreamException;

/**
 * XML {@code others.xml} metadata imperative writer.
 * <p>
 * This object is not thread safe and depends on order of method calls.
 * </p>
 * @since 0.6
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class XmlOthers implements Closeable {

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
     * @param file Path to write other.xml
     */
    public XmlOthers(final Path file) {
        this(new XmlFile(file));
    }

    /**
     * Primary ctor.
     * @param xml Xml file
     */
    private XmlOthers(final XmlFile xml) {
        this.xml = xml;
        this.packages = new XmlPackagesFile(
            xml, "otherdata", "http://linux.duke.edu/metadata/other"
        );
    }

    /**
     * Start packages.
     * @return Self
     * @throws XMLStreamException On error
     */
    public XmlOthers startPackages() throws XMLStreamException {
        this.packages.startPackages();
        return this;
    }

    /**
     * Add new package.
     * @param name Package name
     * @param arch Package arch
     * @param checksum Package checksum
     * @return Package writer
     * @throws XMLStreamException On error
     */
    public XmlOthers.Package addPackage(final String name, final String arch, final String checksum)
        throws XMLStreamException {
        this.xml.writeStartElement("package");
        this.xml.writeAttribute("pkgid", checksum);
        this.xml.writeAttribute("name", name);
        this.xml.writeAttribute("arch", arch);
        return new XmlOthers.Package(this, this.xml);
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
         * Others reference.
         */
        private final XmlOthers others;

        /**
         * XML writer.
         */
        private final XmlFile xml;

        /**
         * Ctor.
         * @param others Others reference
         * @param xml XML writer
         */
        Package(final XmlOthers others, final XmlFile xml) {
            this.others = others;
            this.xml = xml;
        }

        /**
         * Add version.
         * @param eopch Epoch seconds
         * @param ver Version string
         * @param rel Release name
         * @return Self
         * @throws XMLStreamException On error
         */
        public Package version(final int eopch, final String ver, final String rel)
            throws XMLStreamException {
            this.xml.writeEmptyElement("version");
            this.xml.writeAttribute("epoch", String.valueOf(eopch));
            this.xml.writeAttribute("ver", ver);
            this.xml.writeAttribute("rel", rel);
            return this;
        }

        /**
         * Add changelog.
         * @param changelogs List of changelog items
         * @return Self
         * @throws XMLStreamException On error
         * @todo #82:30min Continue changelog implementation.
         *  Changelog info is composed of a sequence of entries (as seen in
         *  https://rpm-packaging-guide.github.io/#packaging-software ,changelog
         *  section). This information will be extracted from package headers
         *  and sent here to be parsed and added to the others.xml file. After
         *  implementing this enable test on XmlOthersTest.
         */
        public Package changelog(final List<String> changelogs)
            throws XMLStreamException {
            this.xml.writeStartElement("changelog");
            this.xml.writeCharacters("NOT_IMPLEMENTED");
            this.xml.writeEndElement();
            return this;
        }

        /**
         * Adds changelog tag to others.xml file.
         * @param author Changelog author
         * @param epoch Epoch seconds
         * @param content Changelog content
         * @return Self
         * @throws XMLStreamException On error
         */
        public Package changelog(final String author, final int epoch, final String content)
            throws XMLStreamException {
            this.xml.writeStartElement("changelog");
            this.xml.writeAttribute("epoch", String.valueOf(epoch));
            this.xml.writeAttribute("author", String.valueOf(author));
            this.xml.writeCharacters(content);
            this.xml.writeEndElement();
            return this;
        }

        /**
         * Close packages.
         * @return Others
         * @throws XMLStreamException On error
         */
        public XmlOthers close() throws XMLStreamException {
            this.xml.writeEndElement();
            this.others.packages.packageClose();
            return this.others;
        }
    }
}
