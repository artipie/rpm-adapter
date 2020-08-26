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
package com.artipie.rpm.hm;

import com.artipie.asto.Storage;
import org.llorllale.cactoos.matchers.MatcherEnvelope;

/**
 * Matcher for checking rempomd.xml file presence and information in the storage.
 *
 * @since 0.10
 * @todo #317:30min Implement StorageHasRepoMd matcher. StorageHasRepoMd should
 *  check if storage contains a valid repomd.xml file for the repository. Checks
 *  that this matcher must perform, at least:
 *  - presence of repomd.xml file
 *  - presence of primary.xml file
 *  - presence of other.xml file
 *  After implementing this, enable tests in StorageHasRepoMdTest.
 */
public final class StorageHasRepoMd extends MatcherEnvelope<Storage> {

    /**
     * Ctor.
     */
    public StorageHasRepoMd() {
        super(stg -> false, desc -> { }, (stg, mis) -> { }
        );
    }
}
