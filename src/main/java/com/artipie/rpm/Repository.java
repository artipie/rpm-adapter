/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.rpm.meta.XmlRepomd;
import com.artipie.rpm.misc.UncheckedFunc;
import com.artipie.rpm.pkg.Metadata;
import com.artipie.rpm.pkg.Package;
import com.artipie.rpm.pkg.PackageOutput;
import com.artipie.rpm.pkg.Repodata;
import com.jcabi.aspects.Tv;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository aggregate {@link PackageOutput}. It accepts package metadata
 * and proxies to all outputs. On complete, it saves summary metadata to
 * {@code repomd.xml} file.
 * @since 0.6
 */
final class Repository implements PackageOutput {

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
     * @param files Metadata files outputs
     * @param digest Digest algorithm
     */
    Repository(final List<Metadata> files, final Digest digest) {
        this.metadata = files;
        this.digest = digest;
    }

    /**
     * Update itself using package metadata.
     * @param pkg Package
     * @return Itself
     * @throws IOException On error
     */
    public Repository update(final Package pkg) throws IOException {
        pkg.save(this, this.digest);
        return this;
    }

    @Override
    public void accept(final Package.Meta meta) throws IOException {
        new PackageOutput.Multiple(this.metadata).accept(meta);
    }

    @Override
    public void close() throws IOException {
        new PackageOutput.Multiple(this.metadata).close();
        Logger.info(this, "repository closed");
    }

    /**
     * Save metadata files and gzip.
     * @param repodata Repository repodata
     * @return All metadata files
     * @throws IOException On error
     */
    public List<Path> save(final Repodata repodata) throws IOException {
        try (XmlRepomd repomd = repodata.createRepomd()) {
            repomd.begin(System.currentTimeMillis() / Tv.THOUSAND);
            final List<Path> outs = this.metadata.stream()
                .map(new UncheckedFunc<>(meta -> meta.save(repodata, this.digest, repomd)))
                .collect(Collectors.toList());
            outs.add(repomd.file());
            return outs;
        }
    }
}
