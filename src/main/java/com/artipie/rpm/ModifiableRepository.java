/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.rpm.misc.UncheckedConsumer;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.InvalidPackageException;
import com.artipie.rpm.pkg.Metadata;
import com.artipie.rpm.pkg.Package;
import com.artipie.rpm.pkg.PackageOutput;
import com.artipie.rpm.pkg.Repodata;
import com.jcabi.log.Logger;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Repository aggregate {@link PackageOutput}, decorator for {@link Repository}. It accepts repo
 * metadata, checks whether package is already exists in metadata files, if not,
 * it adds package data to metadata. On save it also extracts not existing packages from
 * metadatas and saves summary {@code repomd.xml} file.
 * @since 0.6
 */
public final class ModifiableRepository implements PackageOutput {

    /**
     * Origin.
     */
    private final Repository origin;

    /**
     * List of existing packages ids (checksums) from primary.xml.
     */
    private final List<String> existing;

    /**
     * Metadata outputs.
     */
    private final List<Metadata> metadata;

    /**
     * Digest algorithm.
     */
    private final Digest digest;

    /**
     * Ctor.
     * @param existing Existing packages hexes list
     * @param metadata Metadata files
     * @param digest Hashing algorithm
     */
    public ModifiableRepository(final List<String> existing, final List<Metadata> metadata,
        final Digest digest) {
        this.existing = existing;
        this.metadata = metadata;
        this.digest = digest;
        this.origin = new Repository(metadata, digest);
    }

    /**
     * Update itself using package metadata.
     * @param pkg Package
     * @return Itself
     * @throws IOException On error
     */
    public ModifiableRepository update(final FilePackage pkg) throws IOException {
        final String hex = new FileChecksum(pkg.path(), this.digest).hex();
        if (!this.existing.remove(hex)) {
            try {
                this.origin.update(pkg.parsed());
            } catch (final InvalidPackageException ex) {
                Logger.warn(this, "Failed parsing '%s': %[exception]s", pkg.path(), ex);
            }
        }
        return this;
    }

    /**
     * Clears records about packages that does not present in the repository any more
     * from metadata files.
     * @return Itself
     */
    public ModifiableRepository clear() {
        this.metadata.stream().parallel().forEach(
            new UncheckedConsumer<>(meta -> meta.brush(this.existing))
        );
        return this;
    }

    @Override
    public void accept(final Package.Meta meta) throws IOException {
        this.origin.accept(meta);
    }

    @Override
    public void close() {
        this.metadata.stream().parallel().forEach(
            new UncheckedConsumer<>(Closeable::close)
        );
    }

    /**
     * Save metadata files and gzip.
     * @param repodata Repodata
     * @return All metadata files
     * @throws IOException On error
     */
    public List<Path> save(final Repodata repodata) throws IOException {
        return this.origin.save(repodata);
    }
}
