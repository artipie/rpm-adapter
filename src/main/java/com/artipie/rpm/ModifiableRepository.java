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

import com.artipie.rpm.meta.XmlRepomd;
import com.artipie.rpm.misc.UncheckedConsumer;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.InvalidPackageException;
import com.artipie.rpm.pkg.Metadata;
import com.artipie.rpm.pkg.Package;
import com.artipie.rpm.pkg.PackageOutput;
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
     * @param repomd Repomd
     * @param metadata Metadata files
     * @param digest Hashing algorithm
     * @param tmp Temp dir to store metadata
     * @checkstyle ParameterNumberCheck (3 lines)
     */
    public ModifiableRepository(final List<String> existing, final XmlRepomd repomd,
        final List<Metadata> metadata, final Digest digest, final Path tmp) {
        this.existing = existing;
        this.metadata = metadata;
        this.digest = digest;
        this.origin = new Repository(repomd, metadata, digest, tmp);
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
     * @throws IOException On error
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
     * @param naming Naming policy
     * @return All metadata files
     * @throws IOException On error
     */
    public List<Path> save(final NamingPolicy naming) throws IOException {
        return this.origin.save(naming);
    }
}
