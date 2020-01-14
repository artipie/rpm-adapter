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
package com.yegor256.rpm;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.yegor256.asto.Storage;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableSource;
import io.reactivex.rxjava3.core.Single;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.xembly.Directives;

/**
 * Repomd XML file.
 *
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
     *
     * @param stg The storage
     */
    Repomd(final Storage stg) {
        this.storage = stg;
    }

    /**
     * Load or create repomd.xml file.
     *
     * @param file file to load it to.
     * @return Completion or error signal.
     */
    private Completable loadRepomd(final Path file) {
        return this.storage.exists("repodata/repomd.xml")
            .flatMapCompletable(
                exists -> {
                    final Completable res;
                    if (exists) {
                        res = this.storage.load("repodata/repomd.xml", file);
                    } else {
                        res = Completable.fromAction(
                            () -> Files.write(
                                file,
                                // @checkstyle LineLength (1 line)
                                "<repomd xmlns='http://linux.duke.edu/metadata/repo'/>".getBytes()
                            )
                        );
                    }
                    return res;
                });
    }

    /**
     * Update.
     *
     * @param type The type
     * @param act  The act
     * @return Completion or error signal.
     */
    public Completable update(final String type, final Repomd.Act act) {
        return Single.fromCallable(() -> Files.createTempFile("repomd", ".xml"))
            .flatMapCompletable(
                temp -> loadRepomd(temp)
                    .andThen(Single.fromCallable(() -> Files.createTempFile("x", ".data")))
                    .flatMapCompletable(file -> this.performUpdate(type, act, temp, file))
            );
    }

    private CompletableSource performUpdate(String type, Act act, Path temp, Path file) throws IOException {
        final XML xml = new XMLDocument(temp.toFile())
            .registerNs("ns", "http://linux.duke.edu/metadata/repo");
        final List<XML> nodes = xml.nodes(
            String.format("/ns:repomd/data[type='%s']", type)
        );
        final Completable res;
        if (!nodes.isEmpty()) {
            final String location = nodes.get(0).xpath("location/@href").get(0);
            res = this.storage.load(location, file);
        } else {
            res = Completable.complete();
        }
        final String key = String.format("repodata/%s.xml", type);
        final Path gzip = Files.createTempFile("x", ".gz");
        return res.andThen(act.update(file))
            .andThen(Repomd.gzip(file, gzip))
            .andThen(
                Single.just(new Directives()
                    .xpath("/repomd")
                    .addIf("revision").set("1")
                    .xpath(String.format("/repomd/data[type='%s']", type))
                    .remove()
                    .xpath("/repomd")
                    .add("data")
                    .attr("type", type)
                    .add("location")
                    .attr("href", String.format("%s.gz", key))
                    .up())
                    .zipWith(
                        Single.fromCallable(() -> Files.size(file)),
                        (directives, size) ->
                            directives.add("open-size")
                                .set(Files.size(file))
                                .up()
                    )
                    .flatMap(
                        directives ->
                            Single.just(
                                directives.add("size")
                                    .set(Files.size(gzip))
                                    .up()
                            )
                                .flatMap(
                                    directivesWithSize ->
                                        new Checksum(gzip).sha()
                                            .map(checksum ->
                                                directivesWithSize
                                                    .add("checksum")
                                                    .attr("type", "sha256")
                                                    .set(checksum)
                                            )
                                )
                    )
                    .zipWith(
                        new Checksum(file).sha(),
                        (directives, open) -> directives.add("open-checksum")
                            .attr("type", "sha256")
                            .set(open)
                    )
                    .map(directives -> directives
                        .add("timestamp")
                        // @checkstyle MagicNumberCheck (1 line)
                        .set(System.currentTimeMillis() / 1000L)
                        .up()
                    )
            )
            .flatMapCompletable(directives -> this.storage.save(key, file)
                .andThen(this.storage.save(String.format("%s.gz", key), gzip))
                .andThen(new Update(temp).apply(directives))
                .andThen(this.storage.save("repodata/repomd.xml", temp)));
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
            });
    }

    /**
     * The act.
     *
     * @since 0.1
     */
    public interface Act {
        /**
         * Update.
         *
         * @param file The file
         * @return Completion or error signal.
         */
        Completable update(Path file);
    }

}
