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

import org.apache.commons.cli.Option;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Tests for {@link RpmOptions}.
 *
 * @since 0.11
 * @checkstyle LeftCurlyCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class RpmOptionsTest {

    @Test
    void createsCorrectDigestOption() {
        MatcherAssert.assertThat(
            RpmOptions.DIGEST.option(),
            new AllOf<>(
                new ListOf<Matcher<? super Option>>(
                    //@checkstyle LineLengthCheck (5 lines)
                    new MatcherOf<>(opt -> { return "dgst".equals(opt.getArgName()); }),
                    new MatcherOf<>(opt -> { return "d".equals(opt.getOpt()); }),
                    new MatcherOf<>(opt -> { return "(optional, default sha256) configures Digest instance for Rpm: sha256 or sha1".equals(opt.getDescription()); }),
                    new MatcherOf<>(opt -> { return "digest".equals(opt.getLongOpt()); })
                )
            )
        );
    }

    @Test
    void createsCorrectNamingOption() {
        MatcherAssert.assertThat(
            RpmOptions.NAMING_POLICY.option(),
            new AllOf<>(
                new ListOf<Matcher<? super Option>>(
                    //@checkstyle LineLengthCheck (5 lines)
                    new MatcherOf<>(opt -> { return "np".equals(opt.getArgName()); }),
                    new MatcherOf<>(opt -> { return "n".equals(opt.getOpt()); }),
                    new MatcherOf<>(opt -> { return "(optional, default plain) configures NamingPolicy for Rpm: plain, sha256 or sha1".equals(opt.getDescription()); }),
                    new MatcherOf<>(opt -> { return "naming-policy".equals(opt.getLongOpt()); })
                )
            )
        );
    }

    @Test
    void createsCorrectFilelistsOption() {
        MatcherAssert.assertThat(
            RpmOptions.FILELISTS.option(),
            new AllOf<>(
                new ListOf<Matcher<? super Option>>(
                    //@checkstyle LineLengthCheck (5 lines)
                    new MatcherOf<>(opt -> { return "fl".equals(opt.getArgName()); }),
                    new MatcherOf<>(opt -> { return "f".equals(opt.getOpt()); }),
                    new MatcherOf<>(opt -> { return "(optional, default true) includes File Lists for Rpm: true or false".equals(opt.getDescription()); }),
                    new MatcherOf<>(opt -> { return "filelists".equals(opt.getLongOpt()); })
                )
            )
        );
    }

    @Test
    void returnsCorrectDigestName() {
        MatcherAssert.assertThat(
            RpmOptions.DIGEST.optionName(),
            new IsEqual<>("digest")
        );
    }

    @Test
    void returnsCorrectNamingPolicyName() {
        MatcherAssert.assertThat(
            RpmOptions.NAMING_POLICY.optionName(),
            new IsEqual<>("naming-policy")
        );
    }

    @Test
    void returnsCorrectFilistsName() {
        MatcherAssert.assertThat(
            RpmOptions.FILELISTS.optionName(),
            new IsEqual<>("filelists")
        );
    }
}
