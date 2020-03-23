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
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.RxFile;
import com.artipie.asto.rx.RxStorageWrapper;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Single;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Repomd XML file.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class Repomd {

    /**
     * The storage.
     */
    private final Storage storage;

    /**
     * Naming policy.
     */
    private final NamingPolicy policy;

    /**
     * Ctor.
     *
     * @param stg The storage
     * @param policy Naming policy
     */
    Repomd(final Storage stg, final NamingPolicy policy) {
        this.storage = stg;
        this.policy = policy;
    }

    /**
     * Update.
     *
     * @param type The type
     * @param act The act
     * @return Completion or error signal.
     */
    public Completable update(final String type, final Repomd.Act act) {
        return Single.fromCallable(() -> Files.createTempFile("repomd", ".xml"))
            .flatMapCompletable(
                temp -> this.loadRepomd(temp).andThen(this.performUpdate(type, act, temp))
            );
    }

    /**
     * Load or create repomd.xml file.
     *
     * @param file File to load it to.
     * @return Completion or error signal.
     */
    private Completable loadRepomd(final Path file) {
        return SingleInterop.fromFuture(
            this.storage.exists(new Key.From("repodata/repomd.xml"))
        ).flatMapCompletable(
            exists -> {
                final Completable res;
                if (exists) {
                    res = new RxFile(file).save(
                        new RxStorageWrapper(this.storage)
                            .value(new Key.From("repodata/repomd.xml"))
                            .flatMapPublisher(pub -> pub)
                    );
                } else {
                    res = Completable.fromAction(
                        () -> Files.write(
                            file,
                            "<repomd xmlns='http://linux.duke.edu/metadata/repo'/>".getBytes()
                        )
                    );
                }
                return res;
            });
    }

    /**
     * Perform the actual update.
     *
     * @param type The file type
     * @param act Action
     * @param repomd The temp
     * @return Completion or error signal
     */
    private CompletableSource performUpdate(
        final String type,
        final Act act,
        final Path repomd
    ) {
        return Single.fromCallable(() -> Files.createTempFile(type, ".xml"))
            .doOnSuccess(
                file -> this.metaFile(
                    new XMLDocument(repomd.toFile())
                        .registerNs("ns", "http://linux.duke.edu/metadata/repo").nodes(
                        String.format("/ns:repomd/data[type='%s']", type)
                    ),
                    file
                )
            )
            .flatMap(file -> act.update(file).andThen(Single.just(file)))
            .zipWith(
                Single.fromCallable(() -> Files.createTempFile(type, ".xml.gz")),
                (src, gzip) -> Repomd.gzip(src, gzip).andThen(
                    SingleInterop.fromFuture(this.policy.name(type, new RxFile(gzip).flow()))
                        .flatMapCompletable(
                            gzipname -> this.performUpdate(
                                type, repomd, src, gzip, gzipname
                            )
                        )
                    )
                ).flatMapCompletable(self -> self);
    }

    /**
     * Perform the actual update.
     *
     * @param type The file type
     * @param repomd The temp
     * @param src Source metadata file
     * @param gzip Gzipped metadata file
     * @param gzipname Gzip key name
     * @return Completion or error signal
     * @todo #29:30min Refactor Repomd.performUpdate method.
     *  It was hot-fixed by adding additional required parameters,
     *  it's hard to read and understand without reading the whole code.
     *  Most probably this method can be split into multiple methods
     *  or some parts can be moved to another class.
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    @SuppressWarnings("PMD.ExcessiveMethodLength")
    private CompletableSource performUpdate(
        final String type,
        final Path repomd,
        final Path src, final Path gzip,
        final String gzipname
    ) {
        final RxStorageWrapper rxsto = new RxStorageWrapper(this.storage);
        final String location = String.format("repodata/%s.xml.gz", gzipname);
        return Single.just(new RepoXml())
            .doOnSuccess(
                rxml -> rxml.base(type, location)
            ).zipWith(
                Single.fromCallable(() -> Files.size(src)), RepoXml::openSize
            ).zipWith(
                Single.fromCallable(() -> Files.size(gzip)), RepoXml::size
            ).zipWith(
                new Checksum(gzip).sha(), RepoXml::checksum
            ).zipWith(
                new Checksum(src).sha(), RepoXml::openChecksum
            ).map(RepoXml::timestamp)
                .flatMapCompletable(
                    rxml ->
                        Completable.mergeArray(
                            rxsto.save(
                                new Key.From(
                                        location
                                ),
                                new RxFile(gzip).flow()
                            )
                        ).andThen(new Update(repomd).apply(rxml))
                            .andThen(
                                Completable.fromAction(
                                    () -> new BlockingStorage(this.storage).save(
                                        new Key.From("repodata/repomd.xml"),
                                        Files.readAllBytes(repomd)
                                    )
                                )
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

    /**
     * Result from storage.
     *
     * @param nodes XML node
     * @param file Path of file
     * @return Result from storage
     */
    private Completable metaFile(
        final List<XML> nodes,
        final Path file) {
        final Completable res;
        if (nodes.isEmpty()) {
            res = Completable.complete();
        } else {
            final String location = nodes.get(0).xpath("location/@href").get(0);
            res = new RxFile(file).save(
                new RxStorageWrapper(this.storage).value(new Key.From(location))
                    .flatMapPublisher(pub -> pub)
            );
        }
        return res;
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
