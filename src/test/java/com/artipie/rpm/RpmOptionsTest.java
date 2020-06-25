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

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RpmOptions}.
 *
 * @since 0.10
 */
@SuppressWarnings("PMD.TooManyMethods")
public class RpmOptionsTest {

    @Test
    void createsCorrectDigestArgName() {
        MatcherAssert.assertThat(
            RpmOptions.DIGEST.option().getArgName(),
            new IsEqual<>("dgst")
        );
    }

    @Test
    void createsCorrectDigestOpt() {
        MatcherAssert.assertThat(
            RpmOptions.DIGEST.option().getOpt(),
            new IsEqual<>("d")
        );
    }

    @Test
    void createsCorrectDigestDescription() {
        MatcherAssert.assertThat(
            RpmOptions.DIGEST.option().getDescription(),
            new IsEqual<>(
                "(optional, default sha256) configures Digest instance for Rpm: sha256 or sha1"
            )
        );
    }

    @Test
    void createsCorrectDigestLongOpt() {
        MatcherAssert.assertThat(
            RpmOptions.DIGEST.option().getLongOpt(),
            new IsEqual<>(
                "digest"
            )
        );
    }

    @Test
    void createsCorrectNamingPolicyArgName() {
        MatcherAssert.assertThat(
            RpmOptions.NAMING_POLICY.option().getArgName(),
            new IsEqual<>("np")
        );
    }

    @Test
    void createsCorrectNamingPolicyOpt() {
        MatcherAssert.assertThat(
            RpmOptions.NAMING_POLICY.option().getOpt(),
            new IsEqual<>("n")
        );
    }

    @Test
    void createsCorrectNamingPolicyDescription() {
        MatcherAssert.assertThat(
            RpmOptions.NAMING_POLICY.option().getDescription(),
            new IsEqual<>(
                "(optional, default plain) configures NamingPolicy for Rpm: plain, sha256 or sha1"
            )
        );
    }

    @Test
    void createsCorrectNamingPolicyLongOpt() {
        MatcherAssert.assertThat(
            RpmOptions.NAMING_POLICY.option().getLongOpt(),
            new IsEqual<>(
                "naming-policy"
            )
        );
    }

    @Test
    void createsCorrectFileListsArgName() {
        MatcherAssert.assertThat(
            RpmOptions.FILELISTS.option().getArgName(),
            new IsEqual<>("fl")
        );
    }

    @Test
    void createsCorrectFileListsOpt() {
        MatcherAssert.assertThat(
            RpmOptions.FILELISTS.option().getOpt(),
            new IsEqual<>("f")
        );
    }

    @Test
    void createsCorrectFileListsDescription() {
        MatcherAssert.assertThat(
            RpmOptions.FILELISTS.option().getDescription(),
            new IsEqual<>(
                "(optional, default true) includes File Lists for Rpm: true or false"
            )
        );
    }

    @Test
    void createsCorrectFileListsLongOpt() {
        MatcherAssert.assertThat(
            RpmOptions.FILELISTS.option().getLongOpt(),
            new IsEqual<>(
                "filelists"
            )
        );
    }
}
