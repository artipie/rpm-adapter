/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.Digest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * Merged xml: merge provided packages into existing xml index.
 * @since 1.5
 */
public interface MergedXml {

    /**
     * Appends provided packages to the index xml.
     * @param packages Packages to append info about
     * @param dgst Digest algorithm
     * @param event Event constant and to append
     * @return Merge result
     * @throws IOException On error
     */
    Result merge(Map<Path, String> packages, Digest dgst, XmlEvent event) throws IOException;

    /**
     * Merge result.
     * @since 1.5
     */
    final class Result {

        /**
         * Items count.
         */
        private final long cnt;

        /**
         * Ids of the items to remove.
         */
        private final Collection<String> ids;

        /**
         * Ctor.
         * @param cnt Items count
         * @param ids Ids of the items to remove
         */
        public Result(final long cnt, final Collection<String> ids) {
            this.cnt = cnt;
            this.ids = ids;
        }

        /**
         * Get packages count.
         * @return Count
         */
        public long count() {
            return this.cnt;
        }

        /**
         * Get packages checksums (ids).
         * @return Checksums
         */
        public Collection<String> checksums() {
            return this.ids;
        }
    }
}
