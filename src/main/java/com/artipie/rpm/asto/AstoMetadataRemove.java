/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Copy;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.misc.UncheckedIOFunc;
import com.artipie.asto.misc.UncheckedIOScalar;
import com.artipie.asto.streams.StorageValuePipeline;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.meta.XmlAlter;
import com.artipie.rpm.meta.XmlMaid;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlPrimaryMaid;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.cactoos.set.SetOf;

/**
 * Removes packages from metadata files.
 * @since 1.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class AstoMetadataRemove {

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
     * @param cnfg Repos config
     */
    public AstoMetadataRemove(final Storage asto, final RepoConfig cnfg) {
        this.asto = asto;
        this.cnfg = cnfg;
    }

    /**
     * Removes packages from metadata xmls. Resulting new xmls are stored into temp location
     * along with checksums and size of un-gziped files. Temp location key as returned in result.
     * @param checksums Checksums of the packages to remove
     * @return Completable action with temp location key
     */
    @SuppressWarnings("rawtypes")
    public CompletionStage<Key> perform(final Collection<String> checksums) {
        final List<CompletableFuture<Void>> res = new ArrayList<>(3);
        final Key.From prefix = new Key.From(UUID.randomUUID().toString());
        final Storage tmpstor = new SubStorage(prefix, this.asto);
        for (final XmlPackage pckg : new XmlPackage.Stream(this.cnfg.filelists())
            .get().collect(Collectors.toList())) {
            res.add(
                CompletableFuture.supplyAsync(() -> pckg).thenCompose(
                    pkg -> this.asto.list(new Key.From("metadata")).thenApply(
                        list -> list.stream()
                            .filter(item -> item.string().contains(pckg.lowercase())).findFirst()
                    ).thenCompose(
                        opt -> {
                            final Key key = new Key.From(pkg.name());
                            final Key tmpkey = new Key.From(prefix, pkg.name());
                            CompletionStage<Void> result = CompletableFuture.allOf();
                            if (opt.isPresent()) {
                                result = new Copy(this.asto, new SetOf<Key>(opt.get()))
                                    .copy(tmpstor)
                                    .thenCompose(nothing -> tmpstor.move(opt.get(), key))
                                    .thenCompose(
                                        ignored -> this.removePackages(pckg, tmpkey, checksums)
                                    )
                                    .thenCompose(
                                        cnt -> new StorageValuePipeline<>(this.asto, tmpkey)
                                            .process(
                                                (inpt, out) -> new XmlAlter.Stream(
                                                    new BufferedInputStream(inpt.get()),
                                                    new BufferedOutputStream(out)
                                                ).pkgAttr(pckg.tag(), String.valueOf(cnt))
                                        )
                                    ).thenCompose(
                                        nothing -> new AstoChecksumAndSize(
                                            this.asto, this.cnfg.digest()
                                        ).calculate(tmpkey)
                                    )
                                    .thenCompose(hex -> new AstoArchive(this.asto).gzip(tmpkey));
                            }
                            return result;
                        }
                    )
                )
            );
        }
        return CompletableFuture.allOf(res.toArray(new CompletableFuture[]{}))
            .thenApply(nothing -> prefix);
    }

    /**
     * Removes packages from metadata file.
     * @param pckg Package type
     * @param key Item key
     * @param checksums Checksums to remove
     * @return Completable action with count of the items left in storage
     */
    private CompletionStage<Long> removePackages(
        final XmlPackage pckg, final Key key, final Collection<String> checksums
    ) {
        return new StorageValuePipeline<Long>(this.asto, key).processWithResult(
            (opt, out) -> {
                final XmlMaid maid;
                final InputStream input = opt.map(new UncheckedIOFunc<>(GZIPInputStream::new))
                    .get();
                if (pckg == XmlPackage.PRIMARY) {
                    maid = new XmlPrimaryMaid.Stream(input, out);
                } else {
                    maid = new XmlMaid.ByPkgidAttr.Stream(input, out);
                }
                return new UncheckedIOScalar<>(() -> maid.clean(checksums)).value();
            }
        );
    }

}
