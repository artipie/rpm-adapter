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

import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Modifiable package.
 * @since 0.8
 */
public final class ModifiableMetadata implements PackageOutput {

    /**
     * Origin.
     */
    private final MetadataFile origin;

    /**
     * Ctor.
     * @param origin Origin
     */
    public ModifiableMetadata(final MetadataFile origin) {
        this.origin = origin;
    }

    /**
     * Deletes info from metadata file by package id (checksum).
     * @param pkgs Packages to delete.
     * @todo #124:30min Implement this method to extract from metadata files data about
     *  packages that are already not presented if the repository. This should be done after
     *  PrimaryOutput, FilelistsOutput and OthersOutput will be able to modify their packages. Also
     *  when this method will be implemented, rpm.Rpm#updateBatchIncrementally(com.artipie.asto.Key)
     *  and use this method to update repos.
     */
    public void delete(final List<String> pkgs) {
        throw new NotImplementedException("Method is not yet implemented");
    }

    @Override
    public void accept(final Package.Meta meta) throws IOException {
        this.origin.accept(meta);
    }

    @Override
    public void close() throws IOException {
        this.origin.close();
    }

}
