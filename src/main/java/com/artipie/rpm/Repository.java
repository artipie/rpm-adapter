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
import com.artipie.rpm.misc.UncheckedFunc;
import com.artipie.rpm.pkg.MetadataFile;
import com.artipie.rpm.pkg.Metadata;
import com.artipie.rpm.pkg.Package;
import com.artipie.rpm.pkg.PackageOutput;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Files;
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
     * Repom XML output.
     */
    private final XmlRepomd repomd;

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
     * @param repomd Repomd XML
     * @param files Metadata files outputs
     * @param digest Digest algorithm
     */
    Repository(final XmlRepomd repomd, final List<Metadata> files, final Digest digest) {
        this.repomd = repomd;
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
     * @param naming Naming policy
     * @return All metadata files
     * @throws IOException On error
     */
    public List<Path> save(final NamingPolicy naming) throws IOException {
        final List<Path> outs = this.metadata.stream()
            .map(new UncheckedFunc<>(meta -> meta.save(naming, this.digest, this.repomd)))
            .collect(Collectors.toList());
        this.repomd.close();
        final Path file = this.repomd.file();
        outs.add(Files.move(file, file.getParent().resolve("repomd.xml")));
        Logger.info(this, "repomd.xml closed");
        return outs;
    }
}
