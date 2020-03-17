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
import com.artipie.asto.fs.RxFile;
import com.artipie.asto.rx.RxStorageWrapper;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.nio.file.Files;

/**
 * The RPM front.
 *
 * First, you make an instance of this class, providing
 * your storage as an argument:
 *
 * <pre> Rpm rpm = new Rpm(storage);</pre>
 *
 * Then, you put your binary RPM artifact to the storage and call
 * {@link Rpm#update(String)}. This method will parse the RPM package
 * and update all the necessary meta-data files. Right after this,
 * your clients will be able to use the package, via {@code yum}:
 *
 * <pre> rpm.update("nginx.rpm").subscribe();</pre>
 *
 * That's it.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @todo #17:30min Create filelist update option
 *  Rpm should not create filelists.xml and filelists.xml.gz if filelistsGen
 *  parameter is not true. Then remove the supressions for PMD.UnusedPrivateField
 *  and PMD.SingularField, and enable the test in RpmTest.
 */
@SuppressWarnings(
    {"PMD.AvoidDuplicateLiterals", "PMD.UnusedPrivateField", "PMD.SingularField"}
    )
public final class Rpm {

    /**
     * The storage.
     */
    private final Storage storage;

    /**
     * Access lock for primary.xml file.
     */
    private final ReactiveLock primary;

    /**
     * Access lock for filelists.xml file.
     */
    private final ReactiveLock filelists;

    /**
     * Access lock for other.xml file.
     */
    private final ReactiveLock other;

    /**
     * RPM files naming policy.
     */
    private final NamingPolicy naming;

    /**
     * Flag for filelists file generation.
     */
    private final boolean filelistsgen;

    /**
     * Ctor.
     * @param stg Storage
     */
    public Rpm(final Storage stg) {
        this(stg, NamingPolicy.DEFAULT, true);
    }

    /**
     * Ctor.
     * @param stg The storage
     * @param naming RPM files naming policy
     * @param filelistsgen Flag to generate the filelist.
     */
    public Rpm(final Storage stg, final NamingPolicy naming, final boolean filelistsgen) {
        this.storage = stg;
        this.other = new ReactiveLock();
        this.filelists = new ReactiveLock();
        this.primary = new ReactiveLock();
        this.naming = naming;
        this.filelistsgen = filelistsgen;
    }

    /**
     * Update the meta info for single artifact.
     *
     * @param key The name of the file just updated
     * @return Completion or error signal.
     */
    public Completable update(final String key) {
        return Single.fromCallable(() -> Files.createTempFile("rpm", ".rpm"))
            .flatMap(
                temp -> new RxFile(temp)
                    .save(
                        new RxStorageWrapper(this.storage).value(new Key.From(key))
                            .flatMapPublisher(pub -> pub)
                    )
                    .andThen(Single.just(new Pkg(temp))))
            .flatMapCompletable(
                pkg -> {
                    final Repomd repomd = new Repomd(this.storage, this.naming);
                    return Completable.concatArray(
                        repomd.update(
                            "primary",
                            new SynchronousAct(
                                file -> new Primary(file).update(key, pkg),
                                this.primary
                            )
                        ),
                        repomd.update(
                            "filelists",
                            new SynchronousAct(
                                file -> new Filelists(file).update(pkg),
                                this.filelists
                            )
                        ),
                        repomd.update(
                            "other",
                            new SynchronousAct(
                                file -> new Other(file).update(pkg),
                                this.other
                            )
                        )
                    );
                }
            );
    }

    /**
     * Batch update RPM files for repository.
     * @param repo Repository key
     * @return Completable action
     */
    public Completable batchUpdate(final String repo) {
        return SingleInterop.fromFuture(this.storage.list(new Key.From(repo)))
            .flatMapObservable(Observable::fromIterable)
            .map(Key::string)
            .filter(key -> key.endsWith(".rpm"))
            .flatMapCompletable(this::update);
    }
}
