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
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RepoConfigFromYamlTest {

    @Test
    void readsSettings() {
        MatcherAssert.assertThat(
            new RepoConfig.FromYaml(
                Yaml.createYamlMappingBuilder().add("digest", "sha1")
                .add("naming-policy", "sha256").add("filelists", "false")
                .add("update", Yaml.createYamlMappingBuilder().add("on", "upload").build()).build()
            ),
            Matchers.allOf(
                new MatcherOf<>(cnfg -> cnfg.digest() == Digest.SHA1),
                new MatcherOf<>(cnfg -> cnfg.naming() == StandardNamingPolicy.SHA256),
                new MatcherOf<>(fromYaml -> !fromYaml.filelists()),
                new MatcherOf<>(cnfg -> cnfg.mode() == RepoConfig.UpdateMode.UPLOAD),
                new MatcherOf<>(new ProcOf<>(cnfg -> !cnfg.cron().isPresent()))
            )
        );
    }

    @Test
    void readsSettingsWithCron() {
        final String cron = "0 * * * *";
        MatcherAssert.assertThat(
            new RepoConfig.FromYaml(
                Yaml.createYamlMappingBuilder()
                    .add(
                        "update",
                        Yaml.createYamlMappingBuilder().add(
                            "on",
                            Yaml.createYamlMappingBuilder().add("cron", cron).build()
                        ).build()
                    ).build()
            ),
            Matchers.allOf(
                new MatcherOf<>(cnfg -> cnfg.mode() == RepoConfig.UpdateMode.CRON),
                new MatcherOf<>(new ProcOf<>(cnfg -> !cnfg.cron().get().equals(cron)))
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
                new MatcherOf<>(new ProcOf<>(RepoConfig.FromYaml::filelists)),
                new MatcherOf<>(cnfg -> cnfg.mode() == RepoConfig.UpdateMode.UPLOAD),
                new MatcherOf<>(new ProcOf<>(cnfg -> !cnfg.cron().isPresent()))
            )
        );
    }
}
