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

import com.artipie.asto.Key;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
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
 * Generates primary.xml metadata file.
 * @since 0.4
 * @checkstyle LineLengthCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class PrimaryProcessor {
    /**
     * Path to write primary.xml.
     */
    private final Path file;

    /**
     * Temporary file to partly processed primary.xml.
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
     * @param file Path to write primary.xml
     * @param dgst Digest algorithm for checksum
     * @throws IOException when could not create temporary file
     * @throws XMLStreamException when could not instantiate streaming XML writer
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    PrimaryProcessor(final Path file, final Digest dgst) throws IOException, XMLStreamException {
        this.file = file;
        this.tmpfile = file.getParent().resolve(
            String.format("%s.part", file.getFileName().toString())
        );
        this.dgst = dgst;
        this.xml = XMLOutputFactory.newFactory().createXMLStreamWriter(
            Files.newOutputStream(this.tmpfile)
        );
        this.lock = new ReactiveLock();
        this.writeHeader();
    }

    /**
     * Generates metadata for the next RPM.
     * @param key Key represents RPM storage path
     * @param pkg RPM for metadata generation
     * @return Completion or error signal
     * @checkstyle ExecutableStatementCountCheck (85 lines)
     * @checkstyle JavaNCSSCheck (85 lines)
     */
    public Completable processNext(final Key key, final Pkg pkg) {
        return this.lock.lock().andThen(Completable.fromAction(
            () -> {
                this.xml.writeStartElement("package");
                this.xml.writeAttribute("type", "rpm");
                this.xml.writeStartElement("name");
                this.xml.writeCharacters(pkg.tag(Header.HeaderTag.NAME));
                this.xml.writeEndElement();
                this.xml.writeStartElement("arch");
                this.xml.writeCharacters(pkg.tag(Header.HeaderTag.ARCH));
                this.xml.writeEndElement();
                this.xml.writeEmptyElement("version");
                this.xml.writeAttribute("epoch", String.valueOf(pkg.num(Header.HeaderTag.EPOCH)));
                this.xml.writeAttribute("ver", pkg.tag(Header.HeaderTag.VERSION));
                this.xml.writeAttribute("rel", pkg.tag(Header.HeaderTag.RELEASE));
            }
        ).andThen(
            new Checksum(pkg.path(), this.dgst).hash()
                .flatMapCompletable(
                    checksum -> Completable.fromAction(
                        () -> {
                            this.xml.writeStartElement("checksum");
                            this.xml.writeAttribute("type", this.dgst.type());
                            this.xml.writeAttribute("pkgid", "YES");
                            this.xml.writeCharacters(checksum);
                            this.xml.writeEndElement();
                            this.xml.writeStartElement("summary");
                            this.xml.writeCharacters(pkg.tag(Header.HeaderTag.SUMMARY));
                            this.xml.writeEndElement();
                            this.xml.writeStartElement("description");
                            this.xml.writeCharacters(pkg.tag(Header.HeaderTag.DESCRIPTION));
                            this.xml.writeEndElement();
                            this.xml.writeStartElement("packager");
                            this.xml.writeCharacters(pkg.tag(Header.HeaderTag.PACKAGER));
                            this.xml.writeEndElement();
                            this.xml.writeStartElement("url");
                            this.xml.writeCharacters(pkg.tag(Header.HeaderTag.URL));
                            this.xml.writeEndElement();
                            this.xml.writeEmptyElement("time");
                            this.xml.writeAttribute("file", String.valueOf(pkg.num(Header.HeaderTag.FILEMTIMES)));
                            this.xml.writeAttribute("build", String.valueOf(pkg.num(Header.HeaderTag.BUILDTIME)));
                        }
                    )
                )
        ).andThen(
            Single.fromCallable(() -> Files.size(pkg.path()))
                .flatMapCompletable(
                    size -> Completable.fromAction(
                        () -> {
                            this.xml.writeEmptyElement("size");
                            this.xml.writeAttribute("package", String.valueOf(size));
                            this.xml.writeAttribute("installed", String.valueOf(pkg.num(Header.HeaderTag.SIZE)));
                            this.xml.writeAttribute("archive", String.valueOf(pkg.num(Header.HeaderTag.ARCHIVESIZE)));
                            this.xml.writeEmptyElement("location");
                            this.xml.writeAttribute("href", key.string());
                            this.xml.writeStartElement("format");
                            this.xml.writeStartElement("http://linux.duke.edu/metadata/rpm", "license");
                            this.xml.writeCharacters(pkg.tag(Header.HeaderTag.LICENSE));
                            this.xml.writeEndElement();
                            this.xml.writeStartElement("http://linux.duke.edu/metadata/rpm", "vendor");
                            this.xml.writeCharacters(pkg.tag(Header.HeaderTag.VENDOR));
                            this.xml.writeEndElement();
                            this.xml.writeStartElement("http://linux.duke.edu/metadata/rpm", "group");
                            this.xml.writeCharacters(pkg.tag(Header.HeaderTag.GROUP));
                            this.xml.writeEndElement();
                            this.xml.writeStartElement("http://linux.duke.edu/metadata/rpm", "buildhost");
                            this.xml.writeCharacters(pkg.tag(Header.HeaderTag.BUILDHOST));
                            this.xml.writeEndElement();
                            this.xml.writeStartElement("http://linux.duke.edu/metadata/rpm", "sourcerpm");
                            this.xml.writeCharacters(pkg.tag(Header.HeaderTag.SOURCERPM));
                            this.xml.writeEndElement();
                            this.xml.writeEmptyElement("http://linux.duke.edu/metadata/rpm", "header-range");
                            this.xml.writeAttribute("start", String.valueOf(pkg.header().getStartPos()));
                            this.xml.writeAttribute("end", String.valueOf(pkg.header().getEndPos()));
                            this.writeProvides(pkg);
                            this.writeRequires(pkg);
                            this.writeFiles(pkg);
                            this.xml.writeEndElement();
                            this.xml.writeEndElement();
                            this.packages = this.packages + 1;
                        }
                    )
                )
        )).doOnTerminate(this.lock::unlock);
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
                    "metadata",
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
        this.xml.writeStartElement("metadata");
        this.xml.writeDefaultNamespace("http://linux.duke.edu/metadata/common");
        this.xml.writeNamespace("rpm", "http://linux.duke.edu/metadata/rpm");
        this.xml.writeAttribute("packages", "-1");
    }

    /**
     * Generate rpm:provides section of metadata.
     * @param pkg RPM for metadata generation
     * @throws XMLStreamException when XML generation causes error
     */
    private void writeProvides(final Pkg pkg) throws XMLStreamException {
        this.xml.writeStartElement("http://linux.duke.edu/metadata/rpm", "provides");
        final String[] provides = (String[]) pkg.header().getEntry(Header.HeaderTag.PROVIDENAME).getValues();
        for (final String name : provides) {
            this.xml.writeStartElement("http://linux.duke.edu/metadata/rpm", "entry");
            this.xml.writeAttribute("name", name);
            this.xml.writeEndElement();
        }
        this.xml.writeEndElement();
    }

    /**
     * Generate rpm:requires section of metadata.
     * @param pkg RPM for metadata generation
     * @throws XMLStreamException when XML generation causes error
     */
    private void writeRequires(final Pkg pkg) throws XMLStreamException {
        this.xml.writeStartElement("http://linux.duke.edu/metadata/rpm", "requires");
        final String[] requires = Arrays.stream(
            (String[]) pkg.header().getEntry(Header.HeaderTag.REQUIRENAME).getValues()
        ).filter(
            name -> !name.startsWith("rpmlib(")
        ).toArray(String[]::new);
        for (final String name : requires) {
            this.xml.writeStartElement("http://linux.duke.edu/metadata/rpm", "entry");
            this.xml.writeAttribute("name", name);
            this.xml.writeEndElement();
        }
        this.xml.writeEndElement();
    }

    /**
     * Write list of files to metadata metadata.
     * @param pkg RPM for metadata generation
     * @throws XMLStreamException when XML generation causes error
     */
    private void writeFiles(final Pkg pkg) throws XMLStreamException {
        final String[] files = (String[]) pkg.header().getEntry(Header.HeaderTag.BASENAMES).getValues();
        final String[] dirs = (String[]) pkg.header().getEntry(Header.HeaderTag.DIRNAMES).getValues();
        final int[] dirsidx = (int[]) pkg.header().getEntry(Header.HeaderTag.DIRINDEXES).getValues();
        final Set<String> dirset = Arrays.stream(dirs).collect(Collectors.toSet());
        for (int idx = 0; idx < files.length; idx += 1) {
            if (files[idx].charAt(0) == '.') {
                continue;
            }
            final String path = String.format("%s%s", dirs[dirsidx[idx]], files[idx]);
            this.xml.writeStartElement("file");
            if (dirset.contains(String.format("%s/", path))) {
                this.xml.writeAttribute("type", "dir");
            }
            this.xml.writeCharacters(path);
            this.xml.writeEndElement();
        }
    }
}
