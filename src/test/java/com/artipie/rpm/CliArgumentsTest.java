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
package com.artipie.rpm;

import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link CliArguments}.
 *
 * @since 0.9
 */
class CliArgumentsTest {

    @Test
    void canParseRepositoryArgument(@TempDir final Path temp) {
        MatcherAssert.assertThat(
            new CliArguments(
                String.format(
                    "%s",
                    temp.getFileName()
                )
            ).repository(),
            new IsEqual<>(temp.getFileName())
        );
    }

    @Test
    void canParseNamingPolicyArgument() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-nsha1"
            ).config().naming(),
            new IsEqual<>(StandardNamingPolicy.SHA1)
        );
    }

    @Test
    void canParseFileListsArgument() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-ffalse"
            ).config().filelists(),
            new IsEqual<>(false)
        );
    }

    @Test
    void canParseDigestArgument() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-dsha1"
            ).config().digest(),
            new IsEqual<>(Digest.SHA1)
        );
    }

    @Test
    void canParseNamingPolicyArgumentWithEquals() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-n=plain"
            ).config().naming(),
            new IsEqual<>(StandardNamingPolicy.PLAIN)
        );
    }

    @Test
    void canParseFileListsArgumentWithEquals() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-f=true"
            ).config().filelists(),
            new IsEqual<>(true)
        );
    }

    @Test
    void canParseDigestArgumentWithEquals() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-d=sha256"
            ).config().digest(),
            new IsEqual<>(Digest.SHA256)
        );
    }

    @Test
    void canParseNamingPolicyArgumentWithLongopt() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-naming-policy=sha256"
            ).config().naming(),
            new IsEqual<>(StandardNamingPolicy.SHA256)
        );
    }

    @Test
    void canParseFileListsArgumentWithLongopt() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-filelists=false"
            ).config().filelists(),
            new IsEqual<>(false)
        );
    }

    @Test
    void canParseDigestArgumentWithLongopt() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-digest=sha1"
            ).config().digest(),
            new IsEqual<>(Digest.SHA1)
        );
    }
}
