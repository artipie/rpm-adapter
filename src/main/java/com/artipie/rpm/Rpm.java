/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
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
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.rpm.meta.XmlRepomd;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.FilelistsOutput;
import com.artipie.rpm.pkg.MetadataFile;
import com.artipie.rpm.pkg.OthersOutput;
import com.artipie.rpm.pkg.PrimaryOutput;
import com.jcabi.aspects.Tv;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The RPM front.
 *
 * First, you make an instance of this class, providing
 * your storage as an argument:
 *
 * <pre> Rpm rpm = new Rpm(storage);</pre>
 *
 * Then, you put your binary RPM artifact to the storage and call
 * {@link Rpm#batchUpdate(Key)}. This method will parse the all RPM packages
 * in repository and update all the necessary meta-data files. Right after this,
 * your clients will be able to use the package, via {@code yum}:
 *
 * <pre> rpm.batchUpdate(new Key.From("rmp-repo"));</pre>
 *
 * @since 0.1
 * @todo #69:30min Add option to exclude `filelists.xml` metadata file from
 *  updates. Som RPM repositories contains too many files in RPM packages,
 *  so it may take too many time to update the filelists. Just add an option
 *  to `Rpm` constructor and exclude filelists output from `Repository`
 *  list of metadata files in `batchUpdate` method.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class Rpm {

    /**
     * Primary storage.
     */
    private final Storage storage;

    /**
     * Naming policy.
     */
    private final NamingPolicy naming;

    /**
     * Digest algorithm for check-sums.
     */
    private final Digest digest;

    /**
     * New Rpm for repository in storage.
     * @param stg The storage which contains repository
     */
    public Rpm(final Storage stg) {
        this(stg, StandardNamingPolicy.PLAIN, Digest.SHA256);
    }

    /**
     * Ctor.
     * @param stg The storage
     * @param naming RPM files naming policy
     * @param dgst Hashing sum computation algorithm
     */
    public Rpm(final Storage stg, final NamingPolicy naming, final Digest dgst) {
        this.storage = stg;
        this.naming = naming;
        this.digest = dgst;
    }

    /**
     * Update the meta info for single artifact.
     *
     * @param key The name of the file just updated
     * @return Completion or error signal.
     * @deprecated use {@link #update(Key)} instead
     */
    @Deprecated
    public Completable update(final String key) {
        return this.update(new Key.From(key));
    }

    /**
     * Update the meta info for single artifact.
     *
     * @param key The name of the file just updated
     * @return Completion or error signal.
     * @deprecated This method calls {@link #batchUpdate(Key)} with parent of the key
     */
    @Deprecated
    public Completable update(final Key key) {
        final String[] parts = key.string().split("/");
        final Key folder;
        if (parts.length == 1) {
            folder = Key.ROOT;
        } else {
            folder = new Key.From(
                Arrays.stream(parts)
                    .limit(parts.length - 1)
                    .toArray(String[]::new)
            );
        }
        return this.batchUpdate(folder);
    }

    /**
     * Batch update RPM files for repository.
     * @param prefix Repository key prefix (String)
     * @return Completable action
     * @deprecated use {@link #batchUpdate(Key)} instead
     */
    @Deprecated
    public Completable batchUpdate(final String prefix) {
        return this.batchUpdate(new Key.From(prefix));
    }

    /**
     * Batch update RPM files for repository.
     * @param prefix Repository key prefix
     * @return Completable action
     */
    public Completable batchUpdate(final Key prefix) {
        final Path tmpdir;
        try {
            tmpdir = Files.createTempDirectory("repo-");
        } catch (final IOException err) {
            throw new IllegalStateException("Failed to create temp dir", err);
        }
        final Vertx vertx = Vertx.vertx();
        final Storage local = new FileStorage(tmpdir, vertx.fileSystem());
        return SingleInterop.fromFuture(this.storage.list(prefix))
            .flatMapObservable(Observable::fromIterable)
            .filter(key -> key.string().endsWith(".rpm"))
            .flatMapSingle(
                key -> {
                    final String file = Paths.get(key.string()).getFileName().toString();
                    return new RxStorageWrapper(this.storage).value(key).flatMapCompletable(
                        content -> new RxStorageWrapper(local).save(new Key.From(file), content)
                    ).andThen(Single.just(new FilePackage(tmpdir.resolve(file))));
                }
            )
            .observeOn(Schedulers.io())
            .reduceWith(
                () -> {
                    final XmlRepomd repomd = new XmlRepomd(
                        Files.createTempFile("repomd-", ".xml")
                    );
                    repomd.begin(System.currentTimeMillis() / Tv.THOUSAND);
                    return new Repository(
                        repomd,
                        Arrays.asList(
                            new MetadataFile(
                                "primary",
                                new PrimaryOutput(Files.createTempFile("primary-", ".xml"))
                                    .start(),
                                repomd
                            ),
                            new MetadataFile(
                                "others",
                                new OthersOutput(Files.createTempFile("others-", ".xml"))
                                    .start(),
                                repomd
                            ),
                            new MetadataFile(
                                "filelists",
                                new FilelistsOutput(Files.createTempFile("filelists-", ".xml"))
                                    .start(),
                                repomd
                            )
                        ),
                        this.digest
                    );
                },
                Repository::update
            )
            .doOnSuccess(rep -> Logger.info(this, "repository updated"))
            .doOnSuccess(Repository::close)
            .doOnSuccess(rep -> Logger.info(this, "repository closed"))
            .flatMapObservable(repo -> Observable.fromIterable(repo.save(this.naming, this.digest)))
            .doOnNext(file -> Files.move(file, tmpdir.resolve(file.getFileName())))
            .flatMapCompletable(
                path -> new RxStorageWrapper(local)
                    .value(new Key.From(path.getFileName().toString()))
                    .flatMapCompletable(
                        content -> new RxStorageWrapper(this.storage).save(
                            new Key.From("repodata", path.getFileName().toString()), content
                        )
                    )
            ).doOnTerminate(
                () -> {
                    Rpm.cleanup(tmpdir);
                    vertx.close();
                }
            );
    }

    /**
     * Cleanup temporary dir.
     * @param dir Directory
     * @throws IOException On error
     */
    private static void cleanup(final Path dir) throws IOException {
        for (final Path item : Files.list(dir).collect(Collectors.toList())) {
            Files.delete(item);
        }
    }
}
