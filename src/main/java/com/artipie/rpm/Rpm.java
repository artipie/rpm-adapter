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
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlPrimaryChecksums;
import com.artipie.rpm.misc.UncheckedFunc;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.InvalidPackageException;
import com.artipie.rpm.pkg.MetadataFile;
import com.artipie.rpm.pkg.ModifiableMetadata;
import com.artipie.rpm.pkg.Package;
import com.artipie.rpm.pkg.PrecedingMetadata;
import com.artipie.rpm.pkg.Repodata;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public final class Rpm {

    /**
     * Primary storage.
     */
    private final Storage storage;

    /**
     * Repository configuration.
     */
    private final RepoConfig config;

    /**
     * New Rpm for repository in storage. Does not include filelists.xml in update.
     * @param stg The storage which contains repository
     */
    public Rpm(final Storage stg) {
        this(stg, StandardNamingPolicy.PLAIN, Digest.SHA256, false);
    }

    /**
     * New Rpm for repository in storage.
     * @param stg The storage which contains repository
     * @param filelists Include file lists in update
     */
    public Rpm(final Storage stg, final boolean filelists) {
        this(stg, StandardNamingPolicy.PLAIN, Digest.SHA256, filelists);
    }

    /**
     * Ctor.
     * @param stg The storage
     * @param naming RPM files naming policy
     * @param dgst Hashing sum computation algorithm
     * @param filelists Include file lists in update
     * @checkstyle ParameterNumberCheck (3 lines)
     */
    public Rpm(final Storage stg, final NamingPolicy naming, final Digest dgst,
        final boolean filelists) {
        this(stg, new RepoConfig.Simple(dgst, naming, filelists));
    }

    /**
     * Ctor.
     * @param storage The storage
     * @param config Repository configuration
     */
    public Rpm(final Storage storage, final RepoConfig config) {
        this.storage = storage;
        this.config = config;
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
        final Path metadir;
        try {
            tmpdir = Files.createTempDirectory("repo-");
            metadir = Files.createTempDirectory("meta-");
        } catch (final IOException err) {
            throw new IllegalStateException("Failed to create temp dir", err);
        }
        final Storage local = new FileStorage(tmpdir);
        return this.filePackageFromRpm(prefix, tmpdir, local)
            .parallel().runOn(Schedulers.io())
            .flatMap(
                file -> {
                    Flowable<Package> parsed;
                    try {
                        parsed = Flowable.just(file.parsed());
                    } catch (final InvalidPackageException ex) {
                        Logger.warn(this, "Failed parsing '%s': %[exception]s", file.path(), ex);
                        parsed = Flowable.empty();
                    }
                    return parsed;
                }
            )
            .sequential().observeOn(Schedulers.io())
            .reduceWith(this::repository, Repository::update)
            .doOnSuccess(rep -> Logger.info(this, "repository updated"))
            .doOnSuccess(Repository::close)
            .doOnSuccess(rep -> Logger.info(this, "repository closed"))
            .flatMapObservable(
                rep -> Observable.fromIterable(
                    rep.save(new Repodata.Temp(this.config.naming(), metadir))
                )
            )
            .flatMapSingle(
                path -> this.moveRepodataToStorage(new FileStorage(metadir), path, prefix)
            )
            .map(path -> path.getFileName().toString())
            .toList().map(HashSet::new)
            .flatMapCompletable(preserve -> this.removeOldMetadata(preserve, prefix))
            .doOnTerminate(
                () -> {
                    Rpm.cleanup(tmpdir);
                    Rpm.cleanup(metadir);
                }
            );
    }

    /**
     * Updates repository incrementally.
     * @param prefix Repo prefix
     * @return Completable action
     */
    public Completable batchUpdateIncrementally(final Key prefix) {
        final Path tmpdir;
        final Path metadir;
        try {
            tmpdir = Files.createTempDirectory("repo-");
            metadir = Files.createTempDirectory("meta-");
        } catch (final IOException err) {
            throw new IllegalStateException("Failed to create temp dir", err);
        }
        final Storage local = new FileStorage(tmpdir);
        return SingleInterop.fromFuture(this.storage.list(prefix))
            .flatMapPublisher(Flowable::fromIterable)
            .filter(key -> key.string().endsWith("xml.gz"))
            .flatMapCompletable(
                key -> new RxStorageWrapper(this.storage)
                    .value(key)
                    .flatMapCompletable(
                        content -> new RxStorageWrapper(local)
                            .save(new Key.From(new KeyLastPart(key).get()), content)
                    )
            ).andThen(Single.fromCallable(() -> this.mdfRepository(tmpdir)))
            .flatMap(
                repo -> this.filePackageFromRpm(prefix, tmpdir, local)
                    .parallel().runOn(Schedulers.io())
                    .sequential().observeOn(Schedulers.io())
                    .reduce(repo, (ignored, pkg) -> repo.update(pkg))
            )
            .doOnSuccess(rep -> Logger.info(this, "repository updated"))
            .doOnSuccess(ModifiableRepository::close)
            .doOnSuccess(rep -> Logger.info(this, "repository closed"))
            .doOnSuccess(ModifiableRepository::clear)
            .doOnSuccess(rep -> Logger.info(this, "repository cleared"))
            .flatMapObservable(
                rep -> Observable.fromIterable(
                    rep.save(new Repodata.Temp(this.config.naming(), metadir))
                )
            )
            .flatMapSingle(
                path -> this.moveRepodataToStorage(new FileStorage(metadir), path, prefix)
            )
            .map(path -> path.getFileName().toString())
            .toList().map(HashSet::new)
            .flatMapCompletable(preserve -> this.removeOldMetadata(preserve, prefix))
            .doOnTerminate(
                () -> Rpm.cleanup(tmpdir)
            );
    }

    /**
     * Removes old metadata.
     * @param preserve Metadata to keep
     * @param prefix Repo prefix
     * @return Completable
     */
    private Completable removeOldMetadata(final Set<String> preserve, final Key prefix) {
        return new RxStorageWrapper(this.storage).list(new Key.From(prefix, "repodata"))
            .flatMapObservable(Observable::fromIterable)
            .filter(item -> !preserve.contains(Paths.get(item.string()).getFileName().toString()))
            .flatMapCompletable(
                item -> new RxStorageWrapper(this.storage).delete(item)
            );
    }

    /**
     * Moves repodata to storage.
     * @param local Local storage
     * @param path Metadata to move
     * @param prefix Repo prefix
     * @return Metadata
     * @todo #240:30min After https://github.com/artipie/asto/issues/201 is fixed, use SubStorage in
     *  this method and get gid of if-else statement inside flatMapCompletable.
     */
    private Single<Path> moveRepodataToStorage(final Storage local, final Path path,
        final Key prefix) {
        return new RxStorageWrapper(local)
            .value(new Key.From(path.getFileName().toString()))
            .flatMapCompletable(
                content -> {
                    final Key key;
                    if (prefix.string().isEmpty()) {
                        key = new Key.From("repodata", path.getFileName().toString());
                    } else {
                        key = new Key.From(prefix, "repodata", path.getFileName().toString());
                    }
                    return new RxStorageWrapper(this.storage).save(key, content);
                }
            ).toSingleDefault(path);
    }

    /**
     * Copies rpms to local storage and constacts {@link FilePackage} instance.
     * @param prefix Repo prefix
     * @param tmpdir Tempdir
     * @param local Local storage
     * @return Flowable of FilePackage
     */
    private Flowable<FilePackage> filePackageFromRpm(
        final Key prefix, final Path tmpdir, final Storage local
    ) {
        return SingleInterop.fromFuture(this.storage.list(prefix))
            .flatMapPublisher(Flowable::fromIterable)
            .filter(key -> key.string().endsWith(".rpm"))
            .flatMapSingle(
                key -> {
                    final String file = new KeyLastPart(key).get();
                    return new RxStorageWrapper(this.storage)
                        .value(key)
                        .flatMapCompletable(
                            content -> new RxStorageWrapper(local).save(new Key.From(file), content)
                        ).andThen(Single.fromCallable(() -> new FilePackage(tmpdir.resolve(file))));
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
        Files.delete(dir);
    }

    /**
     * Get repository for file updates.
     * @return Repository
     */
    private Repository repository() {
        return new Repository(
            new XmlPackage.Stream(this.config.filelists()).get().map(
                new UncheckedFunc<XmlPackage, MetadataFile, IOException>(
                    item -> new MetadataFile(item, item.output().start())
                )
            ).collect(Collectors.toList()),
            this.config.digest()
        );
    }

    /**
     * Get modifiable repository for file updates.
     * @param dir Temp directory
     * @return Repository
     * @throws IOException On error
     */
    private ModifiableRepository mdfRepository(final Path dir) throws IOException {
        return new ModifiableRepository(
            new PrecedingMetadata.FromDir(XmlPackage.PRIMARY, dir).findAndUnzip().map(
                new UncheckedFunc<>(file -> new XmlPrimaryChecksums(file).read())
            ).orElse(Collections.emptyList()),
            new XmlPackage.Stream(this.config.filelists()).get().map(
                new UncheckedFunc<>(
                    item ->
                        new ModifiableMetadata(
                            new MetadataFile(item, item.output().start()),
                            new PrecedingMetadata.FromDir(item, dir)
                        )
                )
            ).collect(Collectors.toList()),
            this.config.digest()
        );
    }

}
