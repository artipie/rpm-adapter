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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link PackagesCount}.
 *
 * @since 0.11
 */
class PackagesCountTest {

    @Test
    void shouldReadValue() throws Exception {
        MatcherAssert.assertThat(
            new PackagesCount(
                Paths.get("src/test/resources-binary/repodata/primary.xml.example")
            ).value(),
            new IsEqual<>(2)
        );
    }

    @Test
    void shouldFailIfAttributeIsMissing(final @TempDir Path dir) throws Exception {
        final PackagesCount count = new PackagesCount(
            Files.write(dir.resolve("empty.xml"), "".getBytes())
        );
        Assertions.assertThrows(IllegalArgumentException.class, count::value);
    }

    @Test
    void shouldFailIfAttributeIsTooFarFromStart(final @TempDir Path dir) throws Exception {
        final PackagesCount count = new PackagesCount(
            Files.write(
                dir.resolve("big.xml"),
                "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n<metadata packages=\"123\"/>".getBytes()
            )
        );
        Assertions.assertThrows(IllegalArgumentException.class, count::value);
    }
}
