/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.rpm.Digest;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.cactoos.map.MapEntry;

/**
 * Checksums and names of the storage items.
 * @since 1.10
 */
final class AstoChecksumAndName {

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Digest algorithm.
     */
    private final Digest dgst;

    /**
     * Ctor.
     * @param asto Asto storage
     * @param dgst Digest algorithm
     */
    AstoChecksumAndName(final Storage asto, final Digest dgst) {
        this.asto = asto;
        this.dgst = dgst;
    }

    /**
     * Calculate checksum of all the items found by key.
     * @param key Storage key
     * @return Map with item name and checksum
     */
    CompletionStage<Map<String, String>> calculate(final Key key) {
        final RxStorageWrapper rxsto = new RxStorageWrapper(this.asto);
        return rxsto.list(key)
            .flatMapObservable(Observable::fromIterable)
            .flatMapSingle(
                item -> rxsto.value(item).flatMap(
                    cnt -> Single.fromFuture(
                        new ContentDigest(cnt, this.dgst::messageDigest).hex().toCompletableFuture()
                    )
                ).map(hex -> new MapEntry<>(keyPart(key, item), hex))
            ).toMap(MapEntry::getKey, MapEntry::getValue)
            .to(SingleInterop.get());
    }

    /**
     * Key part without initial part.
     * @param exclude Initial key part to exclude
     * @param item Item key
     * @return Item key without part to exclude
     */
    private static String keyPart(final Key exclude, final Key item) {
        String res = item.string();
        if (!exclude.equals(Key.ROOT)) {
            res = item.string().substring(exclude.string().length() + 1);
        }
        return res;
    }

}
