/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

/**
 * XML {@code others.xml} metadata imperative writer.
 * <p>
 * This object is not thread safe and depends on order of method calls.
 * </p>
 * @since 0.8
 * @todo #69:30min Create a unit test to verify that this class
 *  can write `others.xml` file correctly. The example of others.xml can
 *  be found at test resources.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class XmlOthers implements Closeable {

    /**
     * XML factory.
     */
    private static final XMLOutputFactory FACTORY =
        XMLOutputFactory.newInstance();

    /**
     * Temporary file to partly processed {@code filelists.xml}.
     */
    private final Path tmp;

    /**
     * Streaming XML writer.
     */
    private final XMLStreamWriter xml;

    /**
     * Processed packages counter.
     */
    private final AtomicInteger packages;

    /**
     * Ctor.
     * @param file Path to write filelists.xml
     */
    public XmlOthers(final Path file) {
        this(file, XmlOthers.xmlStreamWriter(file), new AtomicInteger());
    }

    /**
     * Primary ctor.
     * @param tmp Temporary file
     * @param xml Xml stream
     * @param packages Packages counter
     */
    private XmlOthers(final Path tmp, final XMLStreamWriter xml, final AtomicInteger packages) {
        this.tmp = tmp;
        this.xml = xml;
        this.packages = packages;
    }

    /**
     * Start packages.
     * @return Self
     * @throws XMLStreamException On error
     */
    public XmlOthers startPackages() throws XMLStreamException {
        this.xml.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
        this.xml.writeStartElement("otherdata");
        this.xml.writeDefaultNamespace("http://linux.duke.edu/metadata/other");
        this.xml.writeAttribute("packages", "-1");
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
        final Path trf = Files.createTempFile("others-", ".xml");
        try {
            this.xml.writeEndElement();
            this.xml.writeEndDocument();
            this.xml.close();
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            final XMLInputFactory factory = XMLInputFactory.newFactory();
            final XMLEventReader reader = new AlterAttributeEventReader(
                factory.createXMLEventReader(Files.newInputStream(this.tmp)),
                "otherdata",
                "packages",
                String.valueOf(this.packages.get())
            );
            transformer.transform(
                new StAXSource(reader),
                new StreamResult(Files.newOutputStream(trf, StandardOpenOption.TRUNCATE_EXISTING))
            );
            Files.move(trf, this.tmp, StandardCopyOption.REPLACE_EXISTING);
        } catch (final XMLStreamException | TransformerException err) {
            throw new IOException("Failed to close", err);
        } finally {
            if (Files.exists(trf)) {
                Files.delete(trf);
            }
        }
    }

    /**
     * New XML stream writer from path.
     * @param path File path
     * @return XML stream writer
     * @todo #69:30min Refactor XML classes. Each XML writer (XmlOthers,
     *  XmlPrimary, XmlFilelists, XmlRepomd) has similar code to construct new
     *  stream writer using factory. Let's refactor this somehow.
     */
    private static XMLStreamWriter xmlStreamWriter(final Path path) {
        try {
            return XmlOthers.FACTORY.createXMLStreamWriter(
                Files.newOutputStream(path), StandardCharsets.UTF_8.name()
            );
        } catch (final XMLStreamException err) {
            throw new IllegalStateException("Failed to create XML stream", err);
        } catch (final IOException err) {
            throw new UncheckedIOException("Failed to open file stream", err);
        }
    }

    /**
     * Package writer.
     * @since 0.8
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
