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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.redline_rpm.header.Header;

public class FileListsProcessor {
    private final Path file;
    private final Path tmpfile;
    private final Digest dgst;
    private final XMLStreamWriter xml;
    private final ReactiveLock lock = new ReactiveLock();
    private int packages = 0;

    public FileListsProcessor(final Path file, final Digest dgst) throws IOException, XMLStreamException {
        this.file = file;
        this.tmpfile = file.getParent().resolve(
            String.format("%s.part", file.getFileName().toString())
        );
        this.dgst = dgst;
        final XMLOutputFactory factory = XMLOutputFactory.newFactory();
        this.xml = factory.createXMLStreamWriter(
            Files.newOutputStream(tmpfile)
        );
        this.writeHeader();
    }

    private void writeHeader() throws XMLStreamException {
        this.xml.writeStartDocument();
        this.xml.writeStartElement("filelists");
        this.xml.writeDefaultNamespace("http://linux.duke.edu/metadata/filelists");
        this.xml.writeAttribute("packages", "-1");
    }

    public Completable processNext(final Pkg pkg) {
        return lock.lock().andThen(
            new Checksum(pkg.path(), this.dgst).hash()
                .flatMapCompletable(
                    checksum -> Completable.fromAction(() -> {
                        this.xml.writeStartElement("package");
                        this.xml.writeAttribute("pkgid", checksum);
                        this.xml.writeAttribute("name", pkg.tag(Header.HeaderTag.NAME));
                        this.xml.writeAttribute("arch", pkg.tag(Header.HeaderTag.ARCH));
                        this.xml.writeEmptyElement("version");
                        this.xml.writeAttribute("epoch", String.valueOf(pkg.num(Header.HeaderTag.EPOCH)));
                        this.xml.writeAttribute("ver", pkg.tag(Header.HeaderTag.VERSION));
                        this.xml.writeAttribute("rel", pkg.tag(Header.HeaderTag.RELEASE));
                        this.xml.writeStartElement("file");
                        this.xml.writeCharacters("/test");
                        this.xml.writeEndElement();
                        this.xml.writeEndElement();
                        this.packages ++;
                    })
                    )
        ).doOnTerminate(lock::unlock);
    }

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
                    factory.createXMLEventReader(Files.newInputStream(tmpfile)),
                    "filelists",
                    "packages",
                    String.valueOf(packages)
                );
                transformer.transform(
                    new StAXSource(reader),
                    new StreamResult(Files.newOutputStream(file, StandardOpenOption.TRUNCATE_EXISTING))
                );
            }
        ).doOnTerminate(() -> Files.deleteIfExists(this.tmpfile));
    }
}
