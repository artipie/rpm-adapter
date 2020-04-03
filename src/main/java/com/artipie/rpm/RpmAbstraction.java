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
import io.reactivex.CompletableSource;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * The RPM front.
 *
 * First, you make an instance of this class, providing
 * your storage as an argument:
 *
 * <pre> Rpm rpm = new Rpm(storage);</pre>
 *
 * Then, you put your binary RPM artifact to the storage and call
 * {@link RpmAbstraction#update(Key)}. This method will parse the RPM package
 * and update all the necessary meta-data files. Right after this,
 * your clients will be able to use the package, via {@code yum}:
 *
 * <pre> rpm.update("nginx.rpm").subscribe();</pre>
 *
 * That's it.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")

public interface RpmAbstraction {

    /**
     * Update the meta info for single artifact.
     *
     * @param key The name of the file just updated
     * @return Completion or error signal.
     */
    Completable update(Key key);

    /**
     * Batch update RPM files for repository.
     *
     * @param prefix Repository key prefix
     * @return Completable action
     */
    Completable batchUpdate(Key prefix);

    /**
     * Processes next Key with RepoUpdater.
     *
     * @param updater RepoUpdater instance
     * @param key Key
     * @return Completable action
     */
    CompletableSource doUpdate(RepoUpdater updater, Key key);

    /**
     * Base implementation for Rpm.
     *
     * @since 0.3.3
     */
    final class Base implements RpmAbstraction {

        /**
         * The storage.
         */
        private final Storage storage;

        /**
         * The vertx instance.
         */
        private final Vertx vertx;

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
         * Hashing sum computation algorithm.
         */
        private final Digest dgst;

        /**
         * Ctor.
         *
         * @param stg Storage
         * @param vertx The Vertx instance
         */
        public Base(final Storage stg, final Vertx vertx) {
            this(stg, vertx, NamingPolicy.DEFAULT, Digest.SHA256);
        }

        /**
         * Ctor.
         *
         * @param stg The storage
         * @param vertx The Vertx instance
         * @param naming RPM files naming policy
         * @param dgst Hashing sum computation algorithm
         * @checkstyle ParameterNumberCheck (10 lines)
         */
        public Base(final Storage stg, final Vertx vertx,
            final NamingPolicy naming, final Digest dgst) {
            this.storage = stg;
            this.vertx = vertx;
            this.naming = naming;
            this.dgst = dgst;
            this.other = new ReactiveLock();
            this.filelists = new ReactiveLock();
            this.primary = new ReactiveLock();
        }

        @Override
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

        @Override
        public Completable batchUpdate(final Key prefix) {
            final RepoUpdater updater = new RepoUpdater(
                this.storage,
                this.vertx.fileSystem(),
                this.naming,
                this.dgst
            );
            return updater.deleteMetadata(prefix)
                .andThen(SingleInterop.fromFuture(this.storage.list(prefix))
                    .flatMapObservable(Observable::fromIterable)
                    .filter(key -> key.string().endsWith(".rpm"))
                    .flatMapCompletable(key -> this.doUpdate(updater, key))
                    .andThen(updater.complete(prefix))
                );
        }

        @Override
        public CompletableSource doUpdate(final RepoUpdater updater, final Key key) {
            return Single.fromCallable(() -> Files.createTempFile("rpm", ".rpm"))
                .flatMap(
                    temp -> new RxFile(temp, this.vertx.fileSystem())
                        .save(
                            new RxStorageWrapper(this.storage).value(key)
                                .flatMapPublisher(pub -> pub)
                        ).andThen(Single.just(new Pkg(temp)))
                ).flatMap(
                    pkg -> updater.processNext(key, pkg).andThen(Single.just(pkg))
                ).flatMapCompletable(
                    pkg -> Completable.fromAction(() -> Files.delete(pkg.path()))
                );
        }
    }
}
