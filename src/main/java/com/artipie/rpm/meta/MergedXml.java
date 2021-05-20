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
