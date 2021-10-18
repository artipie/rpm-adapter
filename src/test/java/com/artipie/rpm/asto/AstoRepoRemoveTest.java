/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.rpm.RepoConfig;
import com.jcabi.matchers.XhtmlMatchers;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AstoRepoRemove}.
 * @since 1.9
 */
class AstoRepoRemoveTest {

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Test config.
     */
    private RepoConfig conf;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.conf = new RepoConfig.Simple();
    }

    @Test
    void createsEmptyRepomdIfStorageIsEmpty() {
        new AstoRepoRemove(this.storage, this.conf).perform().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Storage should have 1 item",
            this.storage.list(Key.ROOT).join(),
            Matchers.iterableWithSize(1)
        );
        MatcherAssert.assertThat(
            "Repomd xml should be created",
            new String(
                new BlockingStorage(this.storage).value(new Key.From("metadata", "repomd.xml")),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='repomd']/*[local-name()='revision']"
            )
        );
    }

}
