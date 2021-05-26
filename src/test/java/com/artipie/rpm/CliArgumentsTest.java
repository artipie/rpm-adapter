/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
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
