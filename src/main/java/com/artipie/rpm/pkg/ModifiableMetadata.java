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
package com.artipie.rpm.pkg;

import com.artipie.rpm.Digest;
import com.artipie.rpm.meta.PackagesCount;
import com.artipie.rpm.meta.XmlAlter;
import com.artipie.rpm.meta.XmlMetaJoin;
import com.artipie.rpm.meta.XmlRepomd;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

/**
 * Modifiable package.
 * @since 0.8
 */
public final class ModifiableMetadata implements Metadata {

    /**
     * Origin.
     */
    private final Metadata origin;

    /**
     * Old metadata file from modifiable repository.
     */
    private final PrecedingMetadata preceding;

    /**
     * Packages count.
     */
    private long cnt;

    /**
     * Ctor.
     * @param origin Origin
     * @param preceding Old metadata from existing repository
     */
    public ModifiableMetadata(final Metadata origin, final PrecedingMetadata preceding) {
        this.origin = origin;
        this.preceding = preceding;
        this.cnt = 0L;
    }

    @Override
    public void brush(final List<String> pkgs) throws IOException {
        final Optional<Path> existed = this.preceding.findAndUnzip();
        if (existed.isPresent()) {
            final Path previous = existed.get();
            if (this.cnt > 0) {
                new XmlMetaJoin(this.origin.output().tag())
                    .merge(this.origin.output().file(), previous);
            } else {
                Files.copy(
                    previous, this.origin.output().file(), StandardCopyOption.REPLACE_EXISTING
                );
            }
            if (pkgs.isEmpty()) {
                this.cnt = this.cnt + new PackagesCount(previous).value();
            } else {
                this.cnt = this.origin.output().maid().clean(pkgs);
            }
        }
        new XmlAlter.File(this.origin.output().file()).pkgAttr(
            this.origin.output().tag(), String.valueOf(this.cnt)
        );
    }

    @Override
    public Path save(final Repodata repodata, final Digest digest,
        final XmlRepomd repomd) throws IOException {
        return this.origin.save(repodata, digest, repomd);
    }

    @Override
    public FileOutput output() {
        return this.origin.output();
    }

    @Override
    public void accept(final Package.Meta meta) throws IOException {
        this.origin.accept(meta);
        this.cnt = this.cnt + 1;
    }

    @Override
    public void close() throws IOException {
        this.origin.close();
    }

    @Override
    public String toString() {
        return this.origin.toString();
    }
}
