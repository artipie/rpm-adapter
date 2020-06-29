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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
     */
    public int value() throws IOException {
        try (FileInputStream fis = new FileInputStream(this.path.toFile())) {
            final BufferedReader reader = new BufferedReader(
                new InputStreamReader(fis, StandardCharsets.UTF_8)
            );
            OptionalInt result = OptionalInt.empty();
            String line;
            while ((line = reader.readLine()) != null) {
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
