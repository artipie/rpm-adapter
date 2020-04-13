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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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
     * Processed packages counter.
     */
    private final AtomicInteger packages;

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
        this.packages = new AtomicInteger();
    }

    /**
     * Start packages.
     * @return Self
     * @throws XMLStreamException On error
     */
    public XmlOthers startPackages() throws XMLStreamException {
        this.xml.writer().writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
        this.xml.writer().writeStartElement("otherdata");
        this.xml.writer().writeDefaultNamespace("http://linux.duke.edu/metadata/other");
        this.xml.writer().writeAttribute("packages", "-1");
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
        this.xml.writer().writeStartElement("package");
        this.xml.writer().writeAttribute("pkgid", checksum);
        this.xml.writer().writeAttribute("name", name);
        this.xml.writer().writeAttribute("arch", arch);
        return new XmlOthers.Package(this, this.xml.writer());
    }

    @Override
    public void close() throws IOException {
        try {
            this.xml.writer().writeEndElement();
            this.xml.writer().writeEndDocument();
            this.xml.writer().close();
            this.xml.alterTag(
                "otherdata",
                "packages",
                String.valueOf(this.packages.get())
            );
        } catch (final XMLStreamException err) {
            throw new IOException("Failed to close", err);
        }
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
        private final XMLStreamWriter xml;

        /**
         * Ctor.
         * @param others Others reference
         * @param xml XML writer
         */
        Package(final XmlOthers others, final XMLStreamWriter xml) {
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
         * @return Self
         * @throws XMLStreamException On error
         * @todo #69:30min Implement changelog method. It's not clear how exaclty it should
         *  be extracted from package headers. Check example RPM packages in test bin resources
         *  and `other.xml` file for this file in test resources.
         */
        public Package changelog() throws XMLStreamException {
            this.xml.writeStartElement("changelog");
            this.xml.writeCharacters("NOT_IMPLEMENTED");
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
            this.others.packages.incrementAndGet();
            return this.others;
        }
    }
}
