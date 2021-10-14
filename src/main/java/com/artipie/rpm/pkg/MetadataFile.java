/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.rpm.Digest;
import com.artipie.rpm.FileChecksum;
import com.artipie.rpm.meta.XmlAlter;
import com.artipie.rpm.meta.XmlException;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlRepomd;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;
import javax.xml.stream.XMLStreamException;

/**
 * Metadata file. It's a decorator for {@link PackageOutput},
 * so it should be used to accept metadata from {@link Package}
 * and it proxies metadata to underlying output. After closing it saves
 * all metadata to {@code repomd.xml}.
 * @since 0.6
 */
public final class MetadataFile implements Metadata {

    /**
     * Metadata type.
     */
    private final XmlPackage type;

    /**
     * Output.
     */
    private final FileOutput out;

    /**
     * Packages count.
     */
    private final AtomicLong cnt;

    /**
     * Ctor.
     * @param type Metadata type
     * @param out Output
     */
    public MetadataFile(final XmlPackage type, final FileOutput out) {
        this.type = type;
        this.out = out;
        this.cnt = new AtomicLong();
    }

    @Override
    public void accept(final Package.Meta meta) throws IOException {
        Logger.debug(this, "accepting %s", this.type);
        this.out.accept(meta);
        this.cnt.incrementAndGet();
    }

    @Override
    public void close() throws IOException {
        this.out.close();
        Logger.info(this, "output %s closed", this.out);
    }

    @Override
    public void brush(final List<String> ids) throws IOException {
        new XmlAlter.File(this.out.file()).pkgAttr(this.out.tag(), String.valueOf(this.cnt.get()));
    }

    @Override
    public Path save(final Repodata repodata, final Digest digest, final XmlRepomd repomd)
        throws IOException {
        final Path open = this.out.file();
        Path gzip = Files.createTempFile(repodata.temp(), "", ".gz");
        MetadataFile.gzip(open, gzip);
        gzip = Files.move(gzip, repodata.metadata(this.type, gzip));
        Logger.info(this, "gzipped %s to %s", open, gzip);
        try (XmlRepomd.Data data = repomd.beginData(this.type.lowercase())) {
            data.gzipChecksum(new FileChecksum(gzip, digest));
            data.openChecksum(new FileChecksum(open, digest));
            data.location(String.format("repodata/%s", gzip.getFileName()));
            data.gzipSize(Files.size(gzip));
            data.openSize(Files.size(open));
        } catch (final XMLStreamException err) {
            throw new XmlException("Failed to update repomd.xml", err);
        }
        Files.delete(open);
        return gzip;
    }

    @Override
    public FileOutput output() {
        return this.out;
    }

    @Override
    public String toString() {
        return String.format("MetadataFile: %s", this.type);
    }

    /**
     * Gzip a file.
     *
     * @param input Source file
     * @param output Target file
     * @throws IOException On error
     */
    private static void gzip(final Path input, final Path output) throws IOException {
        try (InputStream fis = Files.newInputStream(input);
            OutputStream fos = Files.newOutputStream(output);
            GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            // @checkstyle MagicNumberCheck (1 line)
            final byte[] buffer = new byte[1024 * 8];
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
}
