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

import com.artipie.rpm.pkg.Checksum;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * XML {@code repomd.xml} metadata imperative writer.
 * <p>
 * This object is not thread safe and depends on order of method calls.
 * </p>
 * @since 0.6
 * @todo #81:30min Refactor this class so it uses XmlFile instead of using
 *  a XMLStreamWriter and a Path. It is not straightforward because it exposes
 *  the Path via the path() which is now hidden in the XmlFile object.
 *  We must keep the path hidden there while at the same time allow for
 *  Repository to be responsible of triggering the save of the repomd file
 *  after all the metadata files (declared in Rpm) have wrote themselves to
 *  repomd as expected. One solution would be to delegate to the xml files
 *  and thus to XmlFile the act of moving itself in the desired place: in
 *  that case the way other metadata files are saved should be adapted.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class XmlRepomd implements Closeable {

    /**
     * XML factory.
     */
    private static final XMLOutputFactory FACTORY =
        XMLOutputFactory.newInstance();

    /**
     * XML stream writer.
     */
    private final XMLStreamWriter xml;

    /**
     * Output stream.
     */
    private final OutputStream stream;

    /**
     * Repomd path.
     */
    private final Path path;

    /**
     * Ctor.
     * @param path Temporary file path
     */
    public XmlRepomd(final Path path) {
        this(path, XmlRepomd.outputStream(path));
    }

    /**
     * Ctor.
     * @param path Repomd path
     * @param stream Underling stream
     */
    private XmlRepomd(final Path path, final OutputStream stream) {
        this.path = path;
        this.stream = stream;
        this.xml = XmlRepomd.xmlStreamWriter(stream);
    }

    /**
     * Begin repomd.
     * @param timestamp Current timestamp in seconds unix time.
     * @throws XMLStreamException On error
     */
    public void begin(final long timestamp) throws XMLStreamException {
        this.xml.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
        this.xml.writeStartElement("repomd");
        this.xml.writeDefaultNamespace("http://linux.duke.edu/metadata/repo");
        this.xml.writeStartElement("revision");
        this.xml.writeCharacters(String.valueOf(timestamp));
        this.xml.writeEndElement();
    }

    /**
     * Start repomd data.
     * @param type Data type
     * @return Data writer
     * @throws XMLStreamException On error
     */
    public XmlRepomd.Data beginData(final String type) throws XMLStreamException {
        this.xml.writeStartElement("data");
        this.xml.writeAttribute("type", type);
        return new XmlRepomd.Data(this.xml);
    }

    /**
     * Repomd file.
     * @return File path
     */
    public Path file() {
        return this.path;
    }

    @Override
    public void close() throws IOException {
        try {
            this.xml.writeEndElement();
            this.xml.close();
            this.stream.close();
        } catch (final XMLStreamException err) {
            throw new IOException("Failed to close", err);
        }
    }

    /**
     * New stream from path.
     * @param path File path
     * @return Output stream
     */
    private static OutputStream outputStream(final Path path) {
        try {
            return Files.newOutputStream(path);
        } catch (final IOException err) {
            throw new UncheckedIOException("Failed to open file stream", err);
        }
    }

    /**
     * New XML stream writer from path.
     * @param stream Output steam
     * @return XML stream writer
     */
    private static XMLStreamWriter xmlStreamWriter(final OutputStream stream) {
        try {
            return XmlRepomd.FACTORY.createXMLStreamWriter(
                stream, StandardCharsets.UTF_8.name()
            );
        } catch (final XMLStreamException err) {
            throw new IllegalStateException("Failed to create XML stream", err);
        }
    }

    /**
     * Repomd {@code data} updater.
     * @since 0.6
     */
    public static final class Data implements Closeable {

        /**
         * XML stream writer.
         */
        private final XMLStreamWriter xml;

        /**
         * Ctor.
         * @param xml XML stream writer
         */
        private Data(final XMLStreamWriter xml) {
            this.xml = xml;
        }

        /**
         * Add checksum.
         * @param checksum Checksum
         * @throws XMLStreamException On error
         * @throws IOException On checksum error
         */
        public void gzipChecksum(final Checksum checksum) throws XMLStreamException, IOException {
            this.xml.writeStartElement("checksum");
            this.xml.writeAttribute("type", checksum.digest().type());
            this.xml.writeCharacters(checksum.hex());
            this.xml.writeEndElement();
        }

        /**
         * Add open-checksum.
         * @param checksum Checksum
         * @throws XMLStreamException On error
         * @throws IOException On checksum error
         */
        public void openChecksum(final Checksum checksum) throws XMLStreamException, IOException {
            this.xml.writeStartElement("open-checksum");
            this.xml.writeAttribute("type", checksum.digest().type());
            this.xml.writeCharacters(checksum.hex());
            this.xml.writeEndElement();
        }

        /**
         * Add location.
         * @param href Location href
         * @throws XMLStreamException On error
         */
        public void location(final String href) throws XMLStreamException {
            this.xml.writeEmptyElement("location");
            this.xml.writeAttribute("href", href);
        }

        /**
         * Add a timestamp.
         * @param sec Timestamp in seconds unix time
         * @throws XMLStreamException On error
         */
        public void timestamp(final long sec) throws XMLStreamException {
            this.xml.writeStartElement("timestamp");
            this.xml.writeCharacters(Long.toString(sec));
            this.xml.writeEndElement();
        }

        /**
         * Add gzip file size.
         * @param size Size in bytes
         * @throws XMLStreamException On error
         */
        public void gzipSize(final long size) throws XMLStreamException {
            this.xml.writeStartElement("size");
            this.xml.writeCharacters(Long.toString(size));
            this.xml.writeEndElement();
        }

        /**
         * Add open file size.
         * @param size Size in bytes
         * @throws XMLStreamException On error
         */
        public void openSize(final long size) throws XMLStreamException {
            this.xml.writeStartElement("open-size");
            this.xml.writeCharacters(Long.toString(size));
            this.xml.writeEndElement();
        }

        @Override
        public void close() throws IOException {
            try {
                this.xml.writeEndElement();
            } catch (final XMLStreamException err) {
                throw new IOException("Failed to close", err);
            }
        }
    }
}
