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
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redline_rpm.header.Header;

/**
 * Tests for {@link FilePackage.Headers}.
 *
 * @since 0.6.3
 * @todo #75:30min Transform the parseStringHeader test below
 *  to a parameterized test that test each of the asXXX method
 *  of MetaHeader with two test method: one for the tag being
 *  present and one for when it is absent (and default value is
 *  returned).
 */
public final class FilePackageHeadersTest {
    @Test
    public void parseStringHeader(@TempDir final Path unused) {
        final Header.HeaderTag tag = Header.HeaderTag.NAME;
        final String expected = "string value";
        final Header header = new Header();
        header.createEntry(tag, expected);
        MatcherAssert.assertThat(
            new FilePackage.Headers(
                header, unused, Digest.SHA256
            ).header(tag).asString("default string"),
            new IsEqual<>(expected)
        );
    }
}
