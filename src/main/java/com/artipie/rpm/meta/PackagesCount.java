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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Count of packages in metadata file.
 *
 * @since 0.11
 */
public class PackagesCount {

    /**
     * RegEx pattern for packages attribute.
     */
    private static final Pattern ATTR = Pattern.compile("packages=\"(\\d+)\"");

    /**
     * Max number of lines to read from file.
     */
    private static final int MAX_LINES = 10;

    /**
     * File path.
     */
    private final Path path;

    /**
     * Ctor.
     *
     * @param path File path.
     */
    public PackagesCount(final Path path) {
        this.path = path;
    }

    /**
     * Read packages count from `packages` attribute.
     *
     * @return Packages count.
     * @throws IOException In case I/O error occurred reading the file.
     */
    public int value() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(this.path)) {
            OptionalInt result = OptionalInt.empty();
            for (int lines = 0; lines < PackagesCount.MAX_LINES; lines = lines + 1) {
                final String line = reader.readLine();
                if (line == null) {
                    break;
                }
                final Matcher matcher = ATTR.matcher(line);
                if (matcher.find()) {
                    result = OptionalInt.of(Integer.parseInt(matcher.group(1)));
                }
            }
            return result.orElseThrow(
                () -> new IllegalStateException("Failed to find packages attribute")
            );
        }
    }
}
