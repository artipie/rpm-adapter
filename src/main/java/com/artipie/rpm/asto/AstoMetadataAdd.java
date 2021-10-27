/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.misc.UncheckedIOFunc;
import com.artipie.asto.misc.UncheckedScalar;
import com.artipie.asto.streams.StorageValuePipeline;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.meta.MergedXml;
import com.artipie.rpm.meta.MergedXmlPackage;
import com.artipie.rpm.meta.MergedXmlPrimary;
import com.artipie.rpm.meta.XmlAlter;
import com.artipie.rpm.meta.XmlEvent;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.pkg.Package;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.zip.GZIPInputStream;

/**
 * Add rpm packages records to metadata.
 * @since 1.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class AstoMetadataAdd {

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
     * @param asto Asto storage
     * @param cnfg Repository config
     */
    public AstoMetadataAdd(final Storage asto, final RepoConfig cnfg) {
        this.asto = asto;
        this.cnfg = cnfg;
    }

    /**
     * Adds provided packages collection to metadata.
     * @param metas Packages metadata to add
     * @return Completable action with temp key
     */
    @SuppressWarnings("SuspiciousToArrayCall")
    public CompletionStage<Key> perform(final Collection<Package.Meta> metas) {
        final Key prefix = new Key.From(UUID.randomUUID().toString());
        return this.addToPrimary(prefix, metas).thenCompose(
            res -> {
                final CompletableFuture<Void> future;
                if (this.cnfg.filelists()) {
                    future = CompletableFuture.allOf(
                        this.add(prefix, metas, res, XmlPackage.OTHER, new XmlEvent.Other()),
                        this.add(prefix, metas, res, XmlPackage.FILELISTS, new XmlEvent.Filelists())
                    );
                } else {
                    future = this.add(prefix, metas, res, XmlPackage.OTHER, new XmlEvent.Other());
                }
                return future;
            }
        ).thenCompose(
            ignored -> this.asto.list(prefix).thenCompose(
                list -> CompletableFuture.allOf(
                    list.stream().map(
                        item -> new AstoChecksumAndSize(this.asto, this.cnfg.digest())
                            .calculate(item)
                            .thenCompose(nothing -> new AstoArchive(this.asto).gzip(item))
                    ).toArray(CompletableFuture[]::new)
                )
            )
        ).thenApply(nothing -> prefix);
    }

    /**
     * Adds items to primary and returns the result.
     * @param temp Temp location
     * @param metas Packages metadata to add
     * @return Completable action with the result
     */
    private CompletionStage<MergedXml.Result> addToPrimary(
        final Key temp, final Collection<Package.Meta> metas
    ) {
        return this.getExistingKey(XmlPackage.PRIMARY).thenCompose(
            opt -> {
                final Key tempkey = new Key.From(temp, XmlPackage.PRIMARY.name());
                return this.copy(tempkey, opt).thenCompose(
                    nothing -> new StorageValuePipeline<MergedXml.Result>(this.asto, tempkey)
                        .processWithResult(
                            (input, out) -> new UncheckedScalar<>(
                                () -> new MergedXmlPrimary(
                                    input.map(new UncheckedIOFunc<>(GZIPInputStream::new)), out
                                ).merge(metas, new XmlEvent.Primary())
                            ).value()
                        )
                ).thenCompose(
                    res -> new StorageValuePipeline<>(this.asto, tempkey).process(
                        (input, out) -> new XmlAlter.Stream(
                            new BufferedInputStream(input.get()),
                            new BufferedOutputStream(out)
                        ).pkgAttr(XmlPackage.PRIMARY.tag(), String.valueOf(res.count()))
                    ).thenApply(nothing -> res)
                );
            }
        );
    }

    /**
     * Adds packages metadata to metadata file.
     * @param temp Temp location
     * @param metas Packages metadata to add
     * @param primary Result of adding packages to primary xml
     * @param type Metadata type
     * @param event Xml event instance
     * @return COmpletable action
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    private CompletableFuture<Void> add(final Key temp, final Collection<Package.Meta> metas,
        final MergedXml.Result primary, final XmlPackage type, final XmlEvent event) {
        return this.getExistingKey(type).thenCompose(
            opt -> {
                final Key tempkey = new Key.From(temp, type.name());
                return this.copy(tempkey, opt).thenCompose(
                    nothing -> new StorageValuePipeline<>(this.asto, tempkey).process(
                        (input, out) -> new UncheckedScalar<>(
                            () -> new MergedXmlPackage(
                                input.map(new UncheckedIOFunc<>(GZIPInputStream::new)),
                                out, type, primary
                            ).merge(metas, event)
                        ).value()
                    )
                );
            }
        ).toCompletableFuture();
    }

    /**
     * Copy existing metadata file to temp location.
     * @param temp Temp location
     * @param opt Optional metadata key
     * @return Completable action
     */
    private CompletionStage<Void> copy(final Key temp, final Optional<Key> opt) {
        return opt.map(
            key -> this.asto.value(key).thenCompose(val -> this.asto.save(temp, val))
        ).orElse(CompletableFuture.allOf());
    }

    /**
     * Find existing metadata key.
     * @param type Metadata type
     * @return Completable action with the key
     */
    private CompletionStage<Optional<Key>> getExistingKey(final XmlPackage type) {
        return this.asto.list(new Key.From("metadata")).thenApply(
            list -> list.stream().filter(
                item -> item.string().endsWith(String.format("%s.xml.gz", type.lowercase()))
            ).findFirst()
        );
    }
}