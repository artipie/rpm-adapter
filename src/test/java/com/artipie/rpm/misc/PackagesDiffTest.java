/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.misc;

import java.util.Map;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link PackagesDiff}.
 * @since 1.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class PackagesDiffTest {

    @Test
    void returnsItemsToDelete() {
        final Map<String, String> primary = new MapOf<>(
            new MapEntry<String, String>("abc.rpm", "abc-checksum"),
            new MapEntry<String, String>("nginx.rpm", "nginx-checksum")
        );
        final Map<String, String> repo = new MapOf<>(
            new MapEntry<String, String>("httpd.rpm", "httpd-checksum"),
            new MapEntry<String, String>("nginx.rpm", "nginx-checksum"),
            new MapEntry<String, String>("openssh.rpm", "openssh-checksum")
        );
        MatcherAssert.assertThat(
            new PackagesDiff(primary, repo).toDelete().entrySet(),
            Matchers.hasItems(new MapEntry<>("abc.rpm", "abc-checksum"))
        );
    }

    @Test
    void returnsItemsToAdd() {
        final Map<String, String> primary = new MapOf<>(
            new MapEntry<String, String>("abc.rpm", "abc-checksum"),
            new MapEntry<String, String>("nginx.rpm", "nginx-checksum")
        );
        final Map<String, String> repo = new MapOf<>(
            new MapEntry<String, String>("httpd.rpm", "httpd-checksum"),
            new MapEntry<String, String>("nginx.rpm", "nginx-checksum"),
            new MapEntry<String, String>("openssh.rpm", "openssh-checksum"),
            new MapEntry<String, String>("abc.rpm", "abc-other-checksum")
        );
        MatcherAssert.assertThat(
            new PackagesDiff(primary, repo).toAdd().entrySet(),
            Matchers.hasItems(
                new MapEntry<>("abc.rpm", "abc-checksum"),
                new MapEntry<>("httpd.rpm", "httpd-checksum"),
                new MapEntry<>("openssh.rpm", "openssh-checksum")
            )
        );
    }
}
