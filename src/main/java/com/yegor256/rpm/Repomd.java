/**
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
package com.yegor256.rpm;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.yegor256.asto.Storage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.xembly.Directives;

/**
 * Repomd XML file.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class Repomd {

    /**
     * The storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param stg The storage
     */
    Repomd(final Storage stg) {
        this.storage = stg;
    }

    /**
     * Update.
     * @param type The type
     * @param act The act
     * @throws IOException If fails
     */
    public void update(final String type, final Repomd.Act act)
        throws IOException {
        final Path temp = Files.createTempFile("repomd", ".xml");
        if (this.storage.exists("repodata/repomd.xml")) {
            this.storage.load("repodata/repomd.xml", temp);
        } else {
            Files.write(
                temp,
                // @checkstyle LineLength (1 line)
                "<repomd xmlns='http://linux.duke.edu/metadata/repo'/>".getBytes()
            );
        }
        final Path file = Files.createTempFile("x", ".data");
        final XML xml = new XMLDocument(temp.toFile())
            .registerNs("ns", "http://linux.duke.edu/metadata/repo");
        final List<XML> nodes = xml.nodes(
            String.format("/ns:repomd/data[type='%s']", type)
        );
        if (!nodes.isEmpty()) {
            final String location = nodes.get(0).xpath("location/@href").get(0);
            this.storage.load(location, file);
        }
        act.update(file);
        final Path gzip = Files.createTempFile("x", ".gz");
        Repomd.gzip(file, gzip);
        final String checksum = new Checksum(gzip).sha();
        final String key = String.format("repodata/%s.xml", type);
        this.storage.save(key, file);
        this.storage.save(String.format("%s.gz", key), gzip);
        new Update(temp).apply(
            new Directives()
                .xpath("/repomd")
                .addIf("revision").set("1")
                .xpath(String.format("/repomd/data[type='%s']", type))
                .remove()
                .xpath("/repomd")
                .add("data")
                .attr("type", type)
                .add("location")
                .attr("href", String.format("%s.gz", key))
                .up()
                .add("open-size")
                .set(Files.size(file))
                .up()
                .add("size")
                .set(Files.size(gzip))
                .up()
                .add("checksum")
                .attr("type", "sha256")
                .set(checksum)
                .up()
                .add("open-checksum")
                .attr("type", "sha256")
                .set(new Checksum(file).sha())
                .up()
                .add("timestamp")
                // @checkstyle MagicNumberCheck (1 line)
                .set(System.currentTimeMillis() / 1000L)
                .up()
        );
        this.storage.save("repodata/repomd.xml", temp);
    }

    /**
     * Gzip a file.
     * @param input Source file
     * @param output Target file
     * @throws IOException If fails
     */
    private static void gzip(final Path input, final Path output)
        throws IOException {
        try (final FileInputStream fis = new FileInputStream(input.toFile());
            final FileOutputStream fos = new FileOutputStream(output.toFile());
            final GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
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

    /**
     * The act.
     */
    public interface Act {
        /**
         * Update.
         * @param file The file
         * @throws IOException If fails
         */
        void update(Path file) throws IOException;
    }

}
