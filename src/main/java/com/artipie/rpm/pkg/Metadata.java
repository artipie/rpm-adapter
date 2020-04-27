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
import com.artipie.rpm.NamingPolicy;
import com.artipie.rpm.meta.XmlRepomd;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Metadata output.
 * @since 0.8
 */
public interface Metadata extends PackageOutput {

    /**
     * Brushes metadata by cleaning not existing packages and setting packages count.
     * @param ids Ids of the packages to clear
     * @throws IOException When error occurs
     */
    void brash(List<String> ids) throws IOException;

    /**
     * Save metadata to repomd, produce gzipped output.
     * @param naming Naming policy
     * @param digest Digest
     * @param repomd Repomd to update
     * @return Gzip metadata file
     * @throws IOException On error
     */
    Path save(NamingPolicy naming, Digest digest, XmlRepomd repomd)
        throws IOException;

    /**
     * Underling metadata file output.
     * @return File output
     */
    PackageOutput.FileOutput output();

}
