/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
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
import java.io.IOException;
import java.util.List;
import org.redline_rpm.header.Header;

/**
 * RPM package.
 *
 * @since 0.6
 */
public interface Package {

    /**
     * Save package to output using digest.
     * @param out Output to save
     * @param digest Digest to use
     * @throws IOException On error
     */
    void save(PackageOutput out, Digest digest) throws IOException;

    /**
     * Package metadata.
     * @since 0.6
     * @todo #69:30min This class has too many methods, consider refactoring it.
     *  Most probably headers methods can be moved to another Headers (or Tags) object.
     */
    @SuppressWarnings("PMD.TooManyMethods")
    interface Meta {

        /**
         * Read header.
         * @param tag Tag name
         * @param def Default value
         * @return Header value
         */
        String header(Header.HeaderTag tag, String def);

        /**
         * Integer header.
         * @param tag Tag name
         * @param def Default value
         * @return Integer number
         */
        int header(Header.HeaderTag tag, int def);

        /**
         * List of string headers.
         * @param tag Tag name
         * @return List of values
         */
        List<String> headers(Header.HeaderTag tag);

        /**
         * Int headers.
         * @param tag Tag name
         * @return Int array
         */
        int[] intHeaders(Header.HeaderTag tag);

        /**
         * RPM file checksum.
         * @return Checksum
         */
        Checksum checksum();

        /**
         * RPM file size.
         * @return File size
         * @throws IOException On error
         */
        long size() throws IOException;

        /**
         * RPM location href.
         * @return Location string
         */
        String href();

        /**
         * Heaaders range.
         * @return Begin and end values
         */
        int[] range();
    }
}
