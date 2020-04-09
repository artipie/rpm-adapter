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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
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
 * XML {@code filelists.xml} metadata file imperative writer.
 * <p>
 * This object is not thread safe and depends on order of method calls.
 * </p>
 * @since 0.4
 * @todo #69:30min Create a unit test to verify that this class
 *  can write `filelists.xml` file correctly. The example of filelists can
 *  be found at test resources.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class XmlFilelists implements Closeable {

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
    public XmlFilelists(final Path file) {
        this(file, XmlFilelists.xmlStreamWriter(file), new AtomicInteger());
    }

    /**
     * Primary ctor.
     * @param tmp Temporary file
     * @param xml XML writer
     * @param packages Packages counter
     */
    private XmlFilelists(final Path tmp, final XMLStreamWriter xml, final AtomicInteger packages) {
        this.tmp = tmp;
        this.xml = xml;
        this.packages = packages;
    }

    /**
     * Starts processing of RPMs.
     * @return Self
     * @throws XMLStreamException when XML generation causes error
     */
    public XmlFilelists startPackages() throws XMLStreamException {
        this.xml.writeStartDocument("UTF-8", "1.0");
        this.xml.writeStartElement("filelists");
        this.xml.writeDefaultNamespace("http://linux.duke.edu/metadata/filelists");
        this.xml.writeAttribute("packages", "-1");
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
        final Path trf = Files.createTempFile("filelists-", ".xml");
        try {
            this.xml.writeEndElement();
            this.xml.writeEndDocument();
            this.xml.close();
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            final XMLInputFactory factory = XMLInputFactory.newFactory();
            final XMLEventReader reader = new AlterAttributeEventReader(
                factory.createXMLEventReader(Files.newInputStream(this.tmp)),
                "filelists",
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
     */
    private static XMLStreamWriter xmlStreamWriter(final Path path) {
        try {
            return XmlFilelists.FACTORY.createXMLStreamWriter(Files.newOutputStream(path), "UTF-8");
        } catch (final XMLStreamException err) {
            throw new IllegalStateException("Failed to create XML stream", err);
        } catch (final IOException err) {
            throw new UncheckedIOException("Failed to open file stream", err);
        }
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
        private final XMLStreamWriter xml;

        /**
         * Ctor.
         * @param filelists Filelists
         * @param xml XML stream
         */
        Package(final XmlFilelists filelists, final XMLStreamWriter xml) {
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
            this.filelists.packages.incrementAndGet();
            return this.filelists;
        }
    }
}
