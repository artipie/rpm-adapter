/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
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
     */
    interface Meta {
        /**
         * Read header.
         * @param tag Tag name
         * @return Header
         */
        MetaHeader header(Header.HeaderTag tag);

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

    /**
     * Package metadata header.
     * @since 0.6.3
     */
    interface MetaHeader {
        /**
         * String header.
         * @param def Default value
         * @return Header value
         */
        String asString(String def);

        /**
         * Integer header.
         * @param def Default value
         * @return Integer number
         */
        int asInt(int def);

        /**
         * List of strings header.
         * @return List of values
         */
        List<String> asStrings();

        /**
         * Array of ints header.
         * @return Int array
         */
        int[] asInts();
    }
}
