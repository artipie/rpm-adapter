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
package com.artipie.rpm.files;

import com.artipie.rpm.TestResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link Gzip}.
 * @since 0.8
 */
class GzipTest {

    @Test
    void unpacks(final @TempDir Path tmp) throws IOException {
        new Gzip(new TestResource("test.tar.gz").file()).unpackTar(tmp);
        MatcherAssert.assertThat(
            Files.readAllLines(tmp.resolve("test.txt")).iterator().next(),
            new IsEqual<>("hello world")
        );
    }

    @Test
    void unpacksGz(final @TempDir Path tmp) throws IOException {
        final Path res = tmp.resolve("res.xml");
        new Gzip(new TestResource("repodata/primary.xml.gz.example").file()).unpack(res);
        MatcherAssert.assertThat(
            Files.readAllLines(res).toArray(),
            Matchers.arrayContaining(
                Files.readAllLines(
                    new TestResource("repodata/primary.xml.example").file()
                ).toArray()
            )
        );
    }

}
