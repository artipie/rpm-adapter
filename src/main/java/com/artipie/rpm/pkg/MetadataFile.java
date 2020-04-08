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
package com.artipie.rpm.pkg;

import com.artipie.rpm.Digest;
import com.artipie.rpm.FileChecksum;
import com.artipie.rpm.NamingPolicy;
import com.artipie.rpm.meta.XmlRepomd;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import javax.xml.stream.XMLStreamException;

/**
 * Metadata file. It's a decorator for {@link PackageOutput},
 * so it should be used to accept metadata from {@link Package}
 * and it proxies metadata to underlying output. After closing it saves
 * all metadata to {@code repomd.xml}.
 * @since 0.8
 * @todo #69:30min Add a unit test for this class, it should verify that
 *  it updates `repomd.xml` file correctly after save.
 */
public final class MetadataFile implements PackageOutput {

    /**
     * Metadata type.
     */
    private final String type;

    /**
     * Output.
     */
    private final FileOutput output;

    /**
     * Repomd XML.
     */
    private final XmlRepomd repomd;

    /**
     * Ctor.
     * @param type Metadata type
     * @param output Output
     * @param repomd Repomd XML
     */
    public MetadataFile(final String type, final FileOutput output, final XmlRepomd repomd) {
        this.type = type;
        this.output = output;
        this.repomd = repomd;
    }

    @Override
    public void accept(final Package.Meta meta) throws IOException {
        this.output.accept(meta);
    }

    /**
     * Save metadata to repomd, produce gzipped output.
     * @param naming Naming policy
     * @param digest Digest
     * @return Gzip metadata file
     * @throws IOException On error
     */
    public Path save(final NamingPolicy naming, final Digest digest) throws IOException {
        this.output.close();
        Logger.info(this, "output %s closed", this.output);
        final Path open = this.output.file();
        Path gzip = Files.createTempFile(this.type, ".gz");
        MetadataFile.gzip(open, gzip);
        gzip = Files.move(
            gzip,
            gzip.getParent().resolve(String.format("%s.xml.gz", naming.name(this.type, gzip)))
        );
        Logger.info(this, "gzipped %s to %s", open, gzip);
        try (XmlRepomd.Data data = this.repomd.beginData(this.type)) {
            data.gzipChecksum(new FileChecksum(gzip, digest));
            data.openChecksum(new FileChecksum(open, digest));
            data.location(String.format("repodata/%s", gzip.getFileName()));
            data.gzipSize(Files.size(gzip));
            data.openSize(Files.size(open));
        } catch (final XMLStreamException err) {
            throw new IOException("Failed to update repomd.xml", err);
        }
        Files.delete(open);
        return gzip;
    }

    @Override
    public void close() {
        // nothing to close
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
