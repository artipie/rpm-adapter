/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
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
