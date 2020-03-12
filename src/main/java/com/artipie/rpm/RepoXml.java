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

import io.reactivex.Single;
import io.reactivex.SingleSource;
import java.nio.file.Files;
import java.nio.file.Path;
import org.xembly.Directives;

/**
 * RepoXml.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class RepoXml {

    /**
     * The file type.
     */
    private final String type;

    /**
     * Gzip of the temp file.
     */
    private final Path gzip;

    /**
     * The temp file.
     */
    private final Path temp;

    /**
     * Ctor.
     *
     * @param type The file type
     * @param gzip Gzip of the temp file
     * @param temp The temp file
     */
    public RepoXml(
        final String type,
        final Path gzip,
        final Path temp) {
        this.type = type;
        this.gzip = gzip;
        this.temp = temp;
    }

    /**
     * XML directives for repo.
     *
     * @param key Key as file name.
     * @return Xml directives as SingleSource
     */
    public SingleSource<Directives> xmlDirectives(final String key) {
        return Single.just(
            new Directives()
                .xpath("/repomd")
                .addIf("revision").set("1")
                .xpath(String.format("/repomd/data[type='%s']", this.type))
                .remove()
                .xpath("/repomd")
                .add("data")
                .attr("type", this.type)
                .add("location")
                .attr("href", String.format("%s.gz", key))
                .up()
        )
            .zipWith(
                Single.fromCallable(() -> Files.size(this.temp)),
                (directives, size) ->
                    directives.add("open-size")
                        .set(size)
                        .up()
            )
            .zipWith(
                Single.fromCallable(() -> Files.size(this.gzip)),
                (directives, size) ->
                    directives.add("size")
                        .set(size)
                        .up()
            )
            .flatMap(
                directives ->
                    new Checksum(this.gzip).sha()
                        .map(
                            checksum ->
                                directives
                                    .add("checksum")
                                    .attr("type", "sha256")
                                    .set(checksum)
                                    .up()
                        )
            )
            .zipWith(
                new Checksum(this.temp).sha(),
                (directives, open) -> directives.add("open-checksum")
                    .attr("type", "sha256")
                    .set(open)
                    .up()
            )
            .map(
                directives ->
                    directives
                        .add("timestamp")
                        // @checkstyle MagicNumberCheck (1 line)
                        .set(System.currentTimeMillis() / 1000L)
                        .up()
            );
    }
}
