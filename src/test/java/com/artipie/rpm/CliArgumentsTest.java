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
 * @todo #113:30min Add more tests for CliArgumentTests.
 *  Add tests for CliArguments using all arguments and longopt name arguments.
 */
class CliArgumentsTest {

    @Test
    void canParseRepositoryArgument(@TempDir final Path temp) {
        MatcherAssert.assertThat(
            new CliArguments().parsed(
                String.format(
                    "%s",
                    temp.getFileName()
                )
            ).repository(),
            new IsEqual<>(temp.getFileName())
        );
    }

    @Test
    void canParseNamingPolicyArgument(@TempDir final Path temp) {
        MatcherAssert.assertThat(
            new CliArguments().parsed(
                "-nsha1"
            ).naming(),
            new IsEqual<>(StandardNamingPolicy.SHA1)
        );
    }

    @Test
    void canParseFileListsArgument(@TempDir final Path temp) {
        MatcherAssert.assertThat(
            new CliArguments().parsed(
                "-ffalse"
            ).fileLists(),
            new IsEqual<>(false)
        );
    }

    @Test
    void canParseDigestArgument(@TempDir final Path temp) {
        MatcherAssert.assertThat(
            new CliArguments().parsed(
                "-dsha1"
            ).digest(),
            new IsEqual<>(Digest.SHA1)
        );
    }
}
