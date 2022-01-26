/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.Digest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link HeaderTags}.
 * @since 1.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class HeaderTagsTest {

    @Test
    void readsObsoletesNames() throws IOException {
        final Path file = new TestResource("ant-1.9.4-2.el7.noarch.rpm").asPath();
        MatcherAssert.assertThat(
            new HeaderTags(
                new FilePackage.Headers(new FilePackageHeader(file).header(), file, Digest.SHA256)
            ).obsoletes(),
            Matchers.contains("ant-scripts")
        );
    }

    @Test
    void readsObsoletesVer() throws IOException {
        final Path file = new TestResource("ant-1.9.4-2.el7.noarch.rpm").asPath();
        MatcherAssert.assertThat(
            new HeaderTags(
                new FilePackage.Headers(new FilePackageHeader(file).header(), file, Digest.SHA256)
            ).obsoletesVer(),
            Matchers.contains("0:1.9.4-2.el7")
        );
    }

    @Test
    void readsObsoletesFlags() throws IOException {
        final Path file = new TestResource("ant-1.9.4-2.el7.noarch.rpm").asPath();
        MatcherAssert.assertThat(
            new HeaderTags(
                new FilePackage.Headers(new FilePackageHeader(file).header(), file, Digest.SHA256)
            ).obsoletesFlags(),
            Matchers.contains(Optional.of("LT"))
        );
    }

}
