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
import com.artipie.rpm.http.RpmRemove;
import com.artipie.rpm.meta.XmlPackage;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.cactoos.map.MapEntry;

/**
 * Workflow to remove packages from repository.
 * @since 1.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class AstoRepoRemove {

    /**
     * Metadata key.
     */
    private static final Key META = new Key.From("metadata");

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
    public AstoRepoRemove(final Storage asto, final RepoConfig cnfg) {
        this.asto = asto;
        this.cnfg = cnfg;
    }

    /**
     * Performs whole workflow to remove items, listed in {@link RpmRemove#TO_RM} location, from
     * the repository.
     * @return Completable action
     */
    public CompletionStage<Void> perform() {
        return this.checksums().thenCompose(
            list -> new AstoMetadataRemove(this.asto, this.cnfg).perform(list)
        ).thenCompose(
            temp -> new AstoCreateRepomd(this.asto, this.cnfg).perform(temp).thenCompose(
                nothing -> this.newNames(temp).thenCompose(
                    keys -> {
                        final StorageLock lock = new StorageLock(this.asto, AstoRepoRemove.META);
                        return lock.acquire()
                            .thenCompose(ignored -> this.remove(AstoRepoRemove.META))
                            .thenCompose(
                                ignored -> CompletableFuture.allOf(
                                    keys.entrySet().stream()
                                    .map(entry -> this.asto.move(entry.getKey(), entry.getValue()))
                                    .toArray(CompletableFuture[]::new)
                                )
                            ).thenCompose(ignored -> lock.release()).thenCompose(
                                ignored -> this.remove(temp)
                            );
                    }
                )
            )
        ).thenCompose(
            ignored -> this.asto.list(RpmRemove.TO_RM).thenCompose(
                list -> CompletableFuture.allOf(
                    list.stream().map(
                        key -> this.asto.delete(key).thenCompose(
                            nothing -> this.asto.delete(AstoRepoRemove.removeTemp(key))
                        )
                    ).toArray(CompletableFuture[]::new)
                )
            )
        );
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
     * Calculate checksums of the packages to remove and removes items from
     * temp location {@link RpmRemove#TO_RM}.
     * @return Checksums list
     */
    private CompletionStage<List<String>> checksums() {
        final RxStorageWrapper rxsto = new RxStorageWrapper(this.asto);
        return rxsto.list(RpmRemove.TO_RM)
            .flatMapObservable(Observable::fromIterable)
            .map(AstoRepoRemove::removeTemp)
            .flatMapSingle(
                key -> rxsto.value(key).flatMap(
                    val -> Single.fromFuture(
                        new ContentDigest(val, () -> this.cnfg.digest().messageDigest())
                            .hex().toCompletableFuture()
                    )
                )
            ).toList().to(SingleInterop.get());
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
                    final Key.From repomd = new Key.From(temp, AstoRepoRemove.REPOMD);
                    return rxsto.exists(repomd).map(
                        exists -> {
                            if (exists) {
                                map.put(
                                    repomd, new Key.From(AstoRepoRemove.META, AstoRepoRemove.REPOMD)
                                );
                            }
                            return map;
                        }
                    );
                }
            ).to(SingleInterop.get());
    }

    /**
     * Removes first {@link RpmRemove#TO_RM} part from the key.
     * @param key Origin key
     * @return Key without {@link RpmRemove#TO_RM} part
     */
    private static Key removeTemp(final Key key) {
        return new Key.From(key.string().substring(RpmRemove.TO_RM.string().length() + 1));
    }

}
