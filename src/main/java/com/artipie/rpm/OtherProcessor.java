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
package com.artipie.rpm;

import io.reactivex.Completable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import org.redline_rpm.header.Header;

/**
 * Generates other.xml metadata file.
 * @since 0.4
 * @checkstyle LineLengthCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class OtherProcessor {
    /**
     * Path to write other.xml.
     */
    private final Path file;

    /**
     * Temporary file to partly processed other.xml.
     */
    private final Path tmpfile;

    /**
     * Digest algorithm for checksum.
     */
    private final Digest dgst;

    /**
     * Streaming XML writer.
     */
    private final XMLStreamWriter xml;

    /**
     * Lock to prevent multi-threaded writes.
     */
    private final ReactiveLock lock;

    /**
     * Processed packages counter.
     */
    private int packages;

    /**
     * Ctor.
     * @param file Path to write filelists.xml
     * @param dgst Digest algorithm for checksum
     * @throws IOException when could not create temporary file
     * @throws XMLStreamException when could not instantiate streaming XML writer
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    OtherProcessor(final Path file, final Digest dgst) throws IOException, XMLStreamException {
        this.file = file;
        this.tmpfile = file.getParent().resolve(
            String.format("%s.part", file.getFileName().toString())
        );
        this.dgst = dgst;
        this.xml = XMLOutputFactory.newFactory().createXMLStreamWriter(
            Files.newOutputStream(this.tmpfile),
            "UTF-8"
        );
        this.lock = new ReactiveLock();
        this.writeHeader();
    }

    /**
     * Generates metadata for the next RPM.
     * @param pkg RPM for metadata generation
     * @return Completion or error signal
     */
    public Completable processNext(final Pkg pkg) {
        return this.lock.lock().andThen(
            new Checksum(pkg.path(), this.dgst).hash()
                .flatMapCompletable(
                    checksum -> Completable.fromAction(
                        () -> {
                            this.xml.writeStartElement("package");
                            this.xml.writeAttribute("pkgid", checksum);
                            this.xml.writeAttribute("name", pkg.tag(Header.HeaderTag.NAME));
                            this.xml.writeAttribute("arch", pkg.tag(Header.HeaderTag.ARCH));
                            this.xml.writeEmptyElement("version");
                            this.xml.writeAttribute("epoch", String.valueOf(pkg.num(Header.HeaderTag.EPOCH)));
                            this.xml.writeAttribute("ver", pkg.tag(Header.HeaderTag.VERSION));
                            this.xml.writeAttribute("rel", pkg.tag(Header.HeaderTag.RELEASE));
                            this.xml.writeStartElement("changelog");
                            this.xml.writeCharacters("?");
                            this.xml.writeEndElement();
                            this.xml.writeEndElement();
                            this.packages = this.packages + 1;
                        }
                    )
                )
        ).doOnTerminate(this.lock::unlock);
    }

    /**
     * Finishes processing of RPMs (close file and re-format).
     * @return Completion or error signal
     */
    public Completable complete() {
        return Completable.fromAction(
            () -> {
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
                    factory.createXMLEventReader(Files.newInputStream(this.tmpfile)),
                    "otherdata",
                    "packages",
                    String.valueOf(this.packages)
                );
                transformer.transform(
                    new StAXSource(reader),
                    new StreamResult(Files.newOutputStream(this.file, StandardOpenOption.TRUNCATE_EXISTING))
                );
            }
        ).doOnTerminate(() -> Files.deleteIfExists(this.tmpfile));
    }

    /**
     * Starts processing of RPMs.
     * @throws XMLStreamException when XML generation causes error
     */
    private void writeHeader() throws XMLStreamException {
        this.xml.writeStartDocument("UTF-8", "1.0");
        this.xml.writeStartElement("otherdata");
        this.xml.writeDefaultNamespace("http://linux.duke.edu/metadata/other");
        this.xml.writeAttribute("packages", "-1");
    }
}
