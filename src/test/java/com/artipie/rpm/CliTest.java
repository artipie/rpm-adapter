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

<<<<<<< HEAD
import java.io.IOException;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
=======
import org.junit.jupiter.api.Assertions;
>>>>>>> upstream/master
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test case for {@link Cli}.
 *
 * @since 0.6
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
<<<<<<< HEAD
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CliTest {

=======
final class CliTest {
>>>>>>> upstream/master
    @Test
    void testWrongArgumentCount() {
        final IllegalArgumentException err = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Cli.main(new String[]{})
        );
        Assertions.assertEquals(
            err.getMessage(),
            "Expected repository path but got: []"
        );
    }

    @Test
    void testRunWithArgument(@TempDir final Path temp) {
        try {
            Cli.main(new String[]{"-nsha256", "-dsha1", "-ftrue", temp.toString()});
        } catch (final IllegalArgumentException exception) {
            MatcherAssert.assertThat(
                String.format("Exception occurred: %s", exception.getMessage()),
                "Incorrect parameters",
                new IsEqual<>(exception.getMessage())
            );
        }
    }

    @Test
    void testRunWithArgumentWithEquals(@TempDir final Path temp) {
        try {
            Cli.main(new String[]{"-n=sha256", "-d=sha1", "-f=true", temp.toString()});
        } catch (final IllegalArgumentException exception) {
            MatcherAssert.assertThat(
                String.format("Exception occurred: %s", exception.getMessage()),
                "Incorrect parameters",
                new IsEqual<>(exception.getMessage())
            );
        }
    }

    @Test
    void testRunWithArgumentWithEqualsAndLongopt(@TempDir final Path temp) {
        try {
            Cli.main(new String[]{"-nsha256", "-d=sha1", "-filelists=true", temp.toString()});
        } catch (final IllegalArgumentException exception) {
            MatcherAssert.assertThat(
                String.format("Exception occurred: %s", exception.getMessage()),
                "Incorrect parameters",
                new IsEqual<>(exception.getMessage())
            );
        }
    }

    @Test
    void testRunWithArgumentWithLongopt(@TempDir final Path temp) throws IOException {
        try {
            // @checkstyle LineLengthCheck (1 lines)
            Cli.main(new String[]{"-naming-policy=sha256", "-digest=sha1", "-filelists=true", temp.toString()});
        } catch (final IllegalArgumentException exception) {
            MatcherAssert.assertThat(
                String.format("Exception occurred: %s", exception.getMessage()),
                "Incorrect parameters",
                new IsEqual<>(exception.getMessage())
            );
        }
    }
}
