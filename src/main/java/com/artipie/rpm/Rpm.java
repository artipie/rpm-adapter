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
import com.artipie.rpm.files.Gzip;
import com.artipie.rpm.meta.XmlPrimaryChecksums;
import com.artipie.rpm.meta.XmlRepomd;
import com.artipie.rpm.misc.UncheckedConsumer;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.FilelistsOutput;
import com.artipie.rpm.pkg.Metadata;
import com.artipie.rpm.pkg.MetadataFile;
import com.artipie.rpm.pkg.ModifiableMetadata;
import com.artipie.rpm.pkg.OthersOutput;
import com.artipie.rpm.pkg.PrimaryOutput;
import com.jcabi.aspects.Tv;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;

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
     * Naming policy.
     */
    private final NamingPolicy naming;

    /**
     * Digest algorithm for check-sums.
     */
    private final Digest digest;

    /**
     * Include filelists?
     */
    private final boolean filelists;

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
        this.storage = stg;
        this.naming = naming;
        this.digest = dgst;
        this.filelists = filelists;
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
            .flatMapPublisher(Flowable::fromIterable)
            .filter(key -> key.string().endsWith(".rpm"))
            .flatMapSingle(
                key -> {
                    final String file = Paths.get(key.string()).getFileName().toString();
                    return new RxStorageWrapper(this.storage)
                        .value(key)
                        .flatMapCompletable(
                            content -> new RxStorageWrapper(local).save(new Key.From(file), content)
                        ).andThen(Single.fromCallable(() -> new FilePackage(tmpdir.resolve(file))));
                }
            ).parallel().runOn(Schedulers.io())
            .map(FilePackage::parsed)
            .sequential().observeOn(Schedulers.io())
            .reduceWith(this::repository, Repository::update)
            .doOnSuccess(rep -> Logger.info(this, "repository updated"))
            .doOnSuccess(Repository::close)
            .doOnSuccess(rep -> Logger.info(this, "repository closed"))
            .flatMapObservable(repo -> Observable.fromIterable(repo.save(this.naming)))
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
     * Updates repository incrementally.
     * @param prefix Repo prefix
     * @return Completable action
     */
    public Completable updateBatchIncrementally(final Key prefix) {
        final Path tmpdir;
        try {
            tmpdir = Files.createTempDirectory("repo-");
        } catch (final IOException err) {
            throw new IllegalStateException("Failed to create temp dir", err);
        }
        final Vertx vertx = Vertx.vertx();
        final Storage local = new FileStorage(tmpdir, vertx.fileSystem());
        return SingleInterop.fromFuture(this.storage.list(prefix))
            .flatMapPublisher(Flowable::fromIterable)
            .filter(key -> key.string().endsWith(".rpm"))
            .flatMapSingle(
                key -> {
                    final String file = Paths.get(key.string()).getFileName().toString();
                    return new RxStorageWrapper(this.storage)
                        .value(key)
                        .flatMapCompletable(
                            content -> new RxStorageWrapper(local).save(new Key.From(file), content)
                        ).andThen(Single.fromCallable(() -> new FilePackage(tmpdir.resolve(file))));
                }
            )
            .observeOn(Schedulers.io())
            .reduceWith(
                () -> this.mdfRepository(tmpdir, local, prefix), ModifiableRepository::update
            )
            .doOnSuccess(rep -> Logger.info(this, "repository updated"))
            .doOnSuccess(ModifiableRepository::close)
            .doOnSuccess(rep -> Logger.info(this, "repository closed"))
            .doOnSuccess(ModifiableRepository::clear)
            .doOnSuccess(rep -> Logger.info(this, "repository cleared"))
            .flatMapObservable(repo -> Observable.fromIterable(repo.save(this.naming)))
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

    /**
     * Get repository for file updates.
     * @return Repository
     * @throws IOException If IO Exception occurs.
     */
    private Repository repository() throws IOException {
        try {
            return new Repository(
                Rpm.xmlRepomd(), new ArrayList<>(this.metadata().values()), this.digest
            );
        } catch (final XMLStreamException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Get modifiable repository for file updates.
     * @param dir Temp directory
     * @param local Local storage
     * @param key Key
     * @return Repository
     * @throws IOException If IO Exception occurs.
     * @todo 178:30min Try to get rid of blocking operations here, at the same time keep in mind
     *  that we need list of the existing rpm checksums from repomd.xml to start the update.
     */
    private ModifiableRepository mdfRepository(final Path dir, final Storage local, final Key key)
        throws IOException {
        try {
            final Map<String, Path> data = new HashMap<>();
            this.storage.list(key).get().stream()
                .filter(file -> file.string().endsWith(".xml.gz"))
                .forEach(
                    file -> new RxStorageWrapper(local).save(
                        new Key.From(Paths.get(file.string()).getFileName().toString()),
                        new RxStorageWrapper(this.storage).value(file).blockingGet()
                    ).blockingGet()
            );
            this.metadata().keySet().forEach(
                new UncheckedConsumer<>(
                    name -> {
                        final Path metaf = dir.resolve(String.format("%s.old.xml", name));
                        new Gzip(Rpm.meta(dir, name)).unpack(metaf);
                        data.put(name, metaf);
                    }
                )
            );
            return new ModifiableRepository(
                new XmlPrimaryChecksums(data.get("primary")).read(),
                Rpm.xmlRepomd(),
                this.metadata().entrySet().stream()
                    .map(
                        entry -> new ModifiableMetadata(entry.getValue(), data.get(entry.getKey()))
                    ).collect(Collectors.toList()),
                this.digest
            );
        } catch (final XMLStreamException | ExecutionException | InterruptedException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Returns opened repomd.
     * @return XmlRepomd instance
     * @throws IOException On error
     * @throws XMLStreamException On error
     */
    private static XmlRepomd xmlRepomd() throws IOException, XMLStreamException {
        final XmlRepomd repomd = new XmlRepomd(Files.createTempFile("repomd-", ".xml"));
        repomd.begin(System.currentTimeMillis() / Tv.THOUSAND);
        return repomd;
    }

    /**
     * Metadata files list.
     * @return Map with metadata files.
     * @throws IOException On error
     * @todo 178:30min Create enum with metadata file items and get rid of string metadata names in
     *  the project and this method. Each enum item has to have at least metadata tag name and
     *  PackageOutput.FileOutput instance.
     */
    private Map<String, Metadata> metadata() throws IOException {
        final Map<String, Metadata> res = new HashMap<>();
        res.put(
            "primary",
            new MetadataFile(
                "primary",
                new PrimaryOutput(Files.createTempFile("primary-", ".xml")).start()
            )
        );
        res.put(
            "other",
            new MetadataFile(
                "other",
                new OthersOutput(Files.createTempFile("other-", ".xml")).start()
            )
        );
        if (this.filelists) {
            res.put(
                "filelists",
                new MetadataFile(
                    "filelists",
                    new FilelistsOutput(Files.createTempFile("filelists-", ".xml")).start()
                )
            );
        }
        return res;
    }

    /**
     * Searches for the meta file by substring in folder.
     * @param dir Where to look for the file
     * @param substr What to find
     * @return Path to find
     * @throws IOException On error
     */
    private static Path meta(final Path dir, final String substr) throws IOException {
        final Optional<Path> res = Files.walk(dir)
            .filter(
                path -> path.getFileName().toString().endsWith(String.format("%s.xml.gz", substr))
            ).findFirst();
        if (res.isPresent()) {
            return res.get();
        } else {
            throw new IllegalStateException(
                String.format("Metafile %s does not exists in %s", substr, dir.toString())
            );
        }
    }
}
