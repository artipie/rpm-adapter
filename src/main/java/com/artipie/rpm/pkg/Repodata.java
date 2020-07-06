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

import com.artipie.rpm.NamingPolicy;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlRepomd;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Repodata creates repomd and files for resulting repository metadata files.
 * @since 0.11
 */
public interface Repodata {

    /**
     * Creates {@link XmlRepomd} instance.
     * @return Xml repomd
     * @throws IOException On error
     */
    XmlRepomd createRepomd() throws IOException;

    /**
     * Temp dir to store metadata files.
     * @return Path
     */
    Path temp();

    /**
     * Path to save resulting metadata.
     * @param type Xml package type
     * @param gzip Gziped metadata
     * @return Path
     * @throws IOException On error
     */
    Path metadata(XmlPackage type, Path gzip) throws IOException;

    /**
     * Temp repodata.
     * @since 0.11
     */
    final class Temp implements Repodata {

        /**
         * Naming policy.
         */
        private final NamingPolicy policy;

        /**
         * Temp directory.
         */
        private final Path tmp;

        /**
         * Ctor.
         * @param naming Naming policy
         * @param tmp Temp directory
         */
        public Temp(final NamingPolicy naming, final Path tmp) {
            this.policy = naming;
            this.tmp = tmp;
        }

        @Override
        public XmlRepomd createRepomd() throws IOException {
            final Path repomd = this.tmp.resolve("repomd.xml");
            repomd.toFile().createNewFile();
            return new XmlRepomd(repomd);
        }

        @Override
        public Path temp() {
            return this.tmp;
        }

        @Override
        public Path metadata(final XmlPackage type, final Path gzip) throws IOException {
            return this.tmp.resolve(
                String.format("%s.xml.gz", this.policy.name(type.filename(), gzip))
            );
        }
    }

}
