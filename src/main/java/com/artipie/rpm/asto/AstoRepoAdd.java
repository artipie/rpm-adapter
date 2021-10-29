/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.lock.storage.StorageLock;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.http.RpmUpload;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.pkg.Package;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.cactoos.map.MapEntry;

/**
 * Add packages to metadata and repository.
 * @since 1.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class AstoRepoAdd {

    /**
     * Metadata key.
     */
    private static final Key META = new Key.From("repodata");

    /**
     * Repomd xml key.
     */
    private static final Key REPOMD = new Key.From("repomd.xml");

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Repository config.
     */
    private final RepoConfig cnfg;

    /**
     * Ctor.
     * @param asto Abstract storage
     * @param cnfg Repository config
     */
    public AstoRepoAdd(final Storage asto, final RepoConfig cnfg) {
        this.asto = asto;
        this.cnfg = cnfg;
    }

    /**
     * Performs whole workflow to add items, listed in {@link com.artipie.rpm.http.RpmUpload#TO_ADD}
     * location, to the repository and metadata files.
     * @return Completable action
     */
    public CompletionStage<Void> perform() {
        return this.read().thenCompose(
            list -> new AstoMetadataAdd(this.asto, this.cnfg).perform(list)
        ).thenCompose(
            temp -> new AstoCreateRepomd(this.asto, this.cnfg).perform(temp).thenCompose(
                nothing -> this.newNames(temp).thenCompose(
                    keys -> {
                        final StorageLock lock = new StorageLock(this.asto, AstoRepoAdd.META);
                        return lock.acquire()
                            .thenCompose(ignored -> this.remove(AstoRepoAdd.META))
                            .thenCompose(
                                ignored -> CompletableFuture.allOf(
                                    keys.entrySet().stream().map(
                                        entry -> this.asto.move(entry.getKey(), entry.getValue())
                                    ).toArray(CompletableFuture[]::new)
                                )
                            )
                            .thenCompose(
                                ignored -> this.asto.list(RpmUpload.TO_ADD).thenCompose(
                                    list -> CompletableFuture.allOf(
                                        list.stream().map(
                                            key -> this.asto.move(
                                                key, AstoRepoAdd.removeTempPart(key)
                                            )
                                        ).toArray(CompletableFuture[]::new)
                                    )
                                )
                            )
                            .thenCompose(ignored -> lock.release()).thenCompose(
                                ignored -> this.remove(temp)
                            );
                    }
                )
            )
        );
    }

    /**
     * Read new packages metadata.
     * @return Completable action with the list of packages metadata to add
     */
    private CompletionStage<List<Package.Meta>> read() {
        final RxStorageWrapper rxsto = new RxStorageWrapper(this.asto);
        return rxsto.list(RpmUpload.TO_ADD)
            .flatMapObservable(Observable::fromIterable)
            .flatMapSingle(
                key -> Single.fromFuture(
                    new AstoRpmPackage(this.asto, this.cnfg.digest()).packageMeta(
                        key, AstoRepoAdd.removeTempPart(key).string()
                    ).toCompletableFuture()
                )
            ).toList().to(SingleInterop.get());
    }

    /**
     * Removes all items found by the key.
     * @param key Key to remove items
     * @return Completable action
     */
    private CompletableFuture<Void> remove(final Key key) {
        return this.asto.list(key).thenCompose(
            list -> CompletableFuture.allOf(
                list.stream().map(this.asto::delete)
                    .toArray(CompletableFuture[]::new)
            )
        );
    }

    /**
     * Creates map of the storage item in temp location and where is should be moved.
     * @param temp Temp location
     * @return Map of the temp and permanent keys
     */
    private CompletionStage<Map<Key, Key>> newNames(final Key temp) {
        final RxStorageWrapper rxsto = new RxStorageWrapper(this.asto);
        return rxsto.list(temp)
            .flatMapObservable(Observable::fromIterable)
            .filter(
                key -> new XmlPackage.Stream(this.cnfg.filelists()).get()
                    .anyMatch(item -> key.string().endsWith(item.name()))
            )
            .<MapEntry<Key, Key>>flatMapSingle(
                key -> rxsto.value(key).flatMap(
                    val -> Single.fromFuture(
                        new ContentDigest(val, () -> this.cnfg.digest().messageDigest()).hex()
                            .thenApply(
                                hex -> new MapEntry<Key, Key>(
                                    key,
                                    new Key.From(
                                        this.cnfg.naming().fullName(
                                            new XmlPackage.Stream(this.cnfg.filelists()).get()
                                                .filter(item -> key.string().contains(item.name()))
                                                .findFirst().get(),
                                            hex
                                        )
                                    )
                                )
                            ).toCompletableFuture()
                    )
                )
            ).toMap(MapEntry::getKey, MapEntry::getValue)
            .flatMap(
                map -> {
                    final Key.From repomd = new Key.From(temp, AstoRepoAdd.REPOMD);
                    return rxsto.exists(repomd).map(
                        exists -> {
                            if (exists) {
                                map.put(
                                    repomd, new Key.From(AstoRepoAdd.META, AstoRepoAdd.REPOMD)
                                );
                            }
                            return map;
                        }
                    );
                }
            ).to(SingleInterop.get());
    }

    /**
     * Removes first {@link RpmUpload#TO_ADD} part from the key.
     * @param key Origin key
     * @return Key without {@link RpmUpload#TO_ADD} part
     */
    private static Key removeTempPart(final Key key) {
        return new Key.From(key.string().substring(RpmUpload.TO_ADD.string().length() + 1));
    }
}
