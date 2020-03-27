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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.RxFile;
import com.artipie.asto.rx.RxStorageWrapper;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.reactivex.core.file.FileSystem;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

public class RepoUpdater {
    private final RxStorageWrapper stg;
    private final FileSystem fsystem;
    private final NamingPolicy policy;
    private final Digest dgst;
    private final Path pfile;
    private final PrimaryProcessor primary;
    private final Path lfile;
    private final FileListsProcessor filelists;
    private final Path ofile;
    private final OtherProcessor other;

    public RepoUpdater(final Storage stg, final FileSystem fsystem, final NamingPolicy policy, final Digest dgst) {
        this.stg = new RxStorageWrapper(stg);
        this.fsystem = fsystem;
        this.policy = policy;
        this.dgst = dgst;
        try {
            this.pfile = Files.createTempFile("primary", ".xml");
            this.primary = new PrimaryProcessor(this.pfile, dgst);
            this.lfile = Files.createTempFile("filelists", ".xml");
            this.filelists = new FileListsProcessor(this.lfile, dgst);
            this.ofile = Files.createTempFile("other", ".xml");
            this.other = new OtherProcessor(this.ofile, dgst);
        } catch (IOException | XMLStreamException ex) {
            throw new IllegalStateException("Could not create repo updater", ex);
        }
    }

    public Completable processNext(Key key, Pkg pkg) {
        return Completable.concatArray(
            this.primary.processNext(key, pkg),
            this.filelists.processNext(pkg),
            this.other.processNext(pkg)
        );
    }

    public Completable complete() {
        final XMLOutputFactory factory = XMLOutputFactory.newFactory();
        final Path repomd;
        final XMLStreamWriter xml;
        try {
            repomd = Files.createTempFile("repomd", ".xml");
            xml = factory.createXMLStreamWriter(
                Files.newOutputStream(repomd)
            );
        } catch (IOException | XMLStreamException ex) {
            throw new IllegalStateException("Could not complete repo update", ex);
        }
        return Completable.concatArray(
            primary.complete(),
            filelists.complete(),
            other.complete()
        ).andThen(
            Completable.fromAction(
                () -> {
                    xml.writeStartDocument();
                    xml.writeStartElement("repomd");
                    xml.writeDefaultNamespace("http://linux.duke.edu/metadata/repo");
                    xml.writeStartElement("revision");
                    // @checkstyle MagicNumberCheck (1 line)
                    xml.writeCharacters(String.valueOf(System.currentTimeMillis() / 1000L));
                    xml.writeEndElement();
                }
            )
        ).andThen(processType(xml, pfile, "primary")
        ).andThen(processType(xml, lfile, "filelists")
        ).andThen(processType(xml, ofile, "other")
        ).andThen(
            Completable.fromAction(
                () -> {
                    xml.writeEndElement();
                    xml.writeEndDocument();
                    xml.close();
                }
            )
        ).andThen(
            Single.fromCallable(() -> {
                final Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                StringWriter out = new StringWriter();
                transformer.transform(
                    new StAXSource(
                        XMLInputFactory.newFactory().createXMLStreamReader(
                            Files.newInputStream(repomd)
                        )
                    ),
                    new StreamResult(out)
                );
                return out.toString();
            })
        ).flatMapCompletable(
            str ->
                this.stg.save(
                    new Key.From("repodata/repomd.xml"),
                    new Content.From(str.getBytes())
                )
        ).doOnTerminate(() -> {
            Files.delete(this.pfile);
            Files.delete(this.lfile);
            Files.delete(this.ofile);
            Files.delete(repomd);
        });
    }

    public Completable deleteMetadata() {
        return this.stg.list(new Key.From("repodata"))
            .flatMapCompletable(
                keys -> Flowable.fromIterable(keys)
                    .flatMapCompletable(this.stg::delete)
            );
    }

    private Completable processType(final XMLStreamWriter writer, final Path file, final String type) {
        return Single.fromCallable(() -> Files.createTempFile(type, ".xml.gz"))
            .flatMapCompletable(
                gzip -> RepoUpdater.gzip(file, gzip).andThen(
                    SingleInterop.fromFuture(
                        this.policy.name(
                            type,
                            new RxFile(gzip, this.fsystem).flow()
                        )
                    )
                ).flatMapCompletable(
                    gzipname -> {
                        final String location = String.format("repodata/%s.xml.gz", gzipname);
                        return Completable.fromAction(() -> {
                            writer.writeStartElement("data");
                            writer.writeAttribute("type", type);
                            writer.writeEmptyElement("location");
                            writer.writeAttribute("href", location);
                        }).andThen(
                            Single.fromCallable(() -> Files.size(file))
                                .flatMapCompletable(opensize ->
                                    Completable.fromAction(() -> {
                                        writer.writeStartElement("opensize");
                                        writer.writeCharacters(String.valueOf(opensize));
                                        writer.writeEndElement();
                                    })
                                )
                        ).andThen(
                            Single.fromCallable(() -> Files.size(gzip))
                                .flatMapCompletable(size ->
                                    Completable.fromAction(() -> {
                                        writer.writeStartElement("size");
                                        writer.writeCharacters(String.valueOf(size));
                                        writer.writeEndElement();
                                    })
                                )
                        ).andThen(
                            new Checksum(gzip, this.dgst).hash()
                                .flatMapCompletable(checksum ->
                                    Completable.fromAction(() -> {
                                        writer.writeStartElement("checksum");
                                        writer.writeAttribute("type", this.dgst.type());
                                        writer.writeCharacters(String.valueOf(checksum));
                                        writer.writeEndElement();
                                    })
                                )
                        ).andThen(
                            new Checksum(file, this.dgst).hash()
                                .flatMapCompletable(checksum ->
                                    Completable.fromAction(() -> {
                                        writer.writeStartElement("open-checksum");
                                        writer.writeAttribute("type", this.dgst.type());
                                        writer.writeCharacters(String.valueOf(checksum));
                                        writer.writeEndElement();
                                    })
                                )
                        ).andThen(
                            Single.fromCallable(() -> Files.size(file))
                                .flatMapCompletable(opensize ->
                                    Completable.fromAction(() -> {
                                        writer.writeStartElement("opensize");
                                        writer.writeCharacters(String.valueOf(opensize));
                                        writer.writeEndElement();
                                    })
                                )
                        ).andThen(Completable.fromAction(() -> {
                            writer.writeStartElement("timestamp");
                            // @checkstyle MagicNumberCheck (1 line)
                            writer.writeCharacters(String.valueOf(System.currentTimeMillis() / 1000L));
                            writer.writeEndElement();
                            writer.writeEndElement();
                        })).andThen(this.stg.save(
                            new Key.From(location),
                            new Content.From(
                                new RxFile(gzip, this.fsystem).flow()
                            )
                        )).doOnTerminate(() -> Files.delete(gzip));
                    }
                )
            );
    }

    /**
     * Gzip a file.
     *
     * @param input Source file
     * @param output Target file
     * @return Completion or error signal.
     */
    private static Completable gzip(final Path input, final Path output) {
        return Completable.fromAction(
            () -> {
                try (InputStream fis = Files.newInputStream(input);
                     OutputStream fos = Files.newOutputStream(output);
                     GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                    // @checkstyle MagicNumberCheck (1 line)
                    final byte[] buffer = new byte[65_536];
                    while (true) {
                        final int length = fis.read(buffer);
                        if (length < 0) {
                            break;
                        }
                        gzos.write(buffer, 0, length);
                    }
                    gzos.finish();
                }
            }
        );
    }
}
