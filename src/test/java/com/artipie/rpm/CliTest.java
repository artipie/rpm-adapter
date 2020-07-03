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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test case for {@link Cli}.
 *
 * @since 0.6
 * @checkstyle LineLengthCheck (70 lines)
 */
final class CliTest {
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
    void testRunWithCorrectArgument(@TempDir final Path temp) {
        Cli.main(new String[]{"-n=sha256", "-d=sha1", "-f=true", temp.toString()});
    }

    @Test
    void testParseWithWrongArgument(@TempDir final Path temp) {
        final IllegalArgumentException err = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Cli.main(new String[] {"-naming-policy=sha256", "-digest=sha1", "-lists=true", temp.toString()})
        );
        Assertions.assertTrue(err.getMessage().contains("Can't parse arguments"));
    }
}
