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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redline_rpm.header.Header;

/**
 * Tests for {@link FilePackage.Headers}.
 *
 * @since 0.6.3
 */
public final class FilePackageHeadersTest {

    @Test
    public void parseStringHeader(@TempDir final Path unused) throws Exception {
        final Header.HeaderTag tag = Header.HeaderTag.NAME;
        final Header header = new Header();
        final String expected = "string value";
        header.createEntry(tag, expected);
        MatcherAssert.assertThat(
            new FilePackage.Headers(
                header, unused, Digest.SHA256
            ).header(tag).asString("default value"),
            new IsEqual<>(expected)
        );
    }

    @Test
    public void parseStringsHeader(@TempDir final Path unused) throws Exception {
        final Header.HeaderTag tag = Header.HeaderTag.NAME;
        final Header header = new Header();
        final String[] expected = new String[]{"s1", "s2"};
        header.createEntry(tag, expected);
        MatcherAssert.assertThat(
            new FilePackage.Headers(
                header, unused, Digest.SHA256
            ).header(tag).asStrings(),
            new IsEqual<>(Arrays.asList(expected))
        );
    }

    @Test
    public void parseIntHeader(@TempDir final Path unused) throws Exception {
        final Header.HeaderTag tag = Header.HeaderTag.EPOCH;
        final Header header = new Header();
        final int expected = 1;
        header.createEntry(tag, expected);
        MatcherAssert.assertThat(
            new FilePackage.Headers(
                header, unused, Digest.SHA256
            ).header(tag).asInt(0),
            new IsEqual<>(expected)
        );
    }

    @Test
    public void parseIntsHeader(@TempDir final Path unused) throws Exception {
        final Header.HeaderTag tag = Header.HeaderTag.EPOCH;
        final Header header = new Header();
        final int[] expected = new int[]{0, 1};
        header.createEntry(tag, expected);
        MatcherAssert.assertThat(
            new FilePackage.Headers(
                header, unused, Digest.SHA256
            ).header(tag).asInts(),
            new IsEqual<>(expected)
        );
    }

    @Test
    public void parseMissingStringHeader(@TempDir final Path unused) {
        final String expected = "default string value";
        MatcherAssert.assertThat(
            new FilePackage.Headers(
                new Header(), unused, Digest.SHA256
            ).header(Header.HeaderTag.NAME).asString(expected),
            new IsEqual<>(expected)
        );
    }

    @Test
    public void parseMissingIntHeader(@TempDir final Path unused) {
        final int expected = 0;
        MatcherAssert.assertThat(
            new FilePackage.Headers(
                new Header(), unused, Digest.SHA256
            ).header(Header.HeaderTag.EPOCH).asInt(expected),
            new IsEqual<>(expected)
        );
    }

    @Test
    public void failParseInvalidPackageFile(@TempDir final Path tmp) throws Exception {
        final Path invalid = tmp.resolve("invalid.rpm");
        Files.write(invalid, "invalid".getBytes());
        final FilePackage pack = new FilePackage(invalid);
        Assertions.assertThrows(InvalidPackageException.class, pack::parsed);
    }

    @Test
    public void failParseNotExistingPackageFile(@TempDir final Path tmp) {
        final FilePackage pack = new FilePackage(tmp.resolve("not-exists.rpm"));
        Assertions.assertThrows(IOException.class, pack::parsed);
    }
}
