/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm;

import com.amihaiemil.eoyaml.Yaml;
import java.util.Optional;
import org.cactoos.func.ProcOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Test for {@link RepoConfig.FromYaml}.
 * @since 0.10
 */
public final class RepoConfigFromYamlTest {

    @Test
    void readsSettings() {
        MatcherAssert.assertThat(
            new RepoConfig.FromYaml(
                Yaml.createYamlMappingBuilder().add("digest", "sha1")
                .add("naming-policy", "sha256").add("filelists", "false").build()
            ),
            Matchers.allOf(
                new MatcherOf<>(cnfg -> cnfg.digest() == Digest.SHA1),
                new MatcherOf<>(cnfg -> cnfg.naming() == StandardNamingPolicy.SHA256),
                new MatcherOf<>(fromYaml -> !fromYaml.filelists())
            )
        );
    }

    @Test
    void returnsDefaults() {
        MatcherAssert.assertThat(
            new RepoConfig.FromYaml(Optional.empty()),
            Matchers.allOf(
                new MatcherOf<>(cnfg -> cnfg.digest() == Digest.SHA256),
                new MatcherOf<>(cnfg -> cnfg.naming() == StandardNamingPolicy.SHA256),
                new MatcherOf<>(new ProcOf<>(RepoConfig.FromYaml::filelists))
            )
        );
    }
}
