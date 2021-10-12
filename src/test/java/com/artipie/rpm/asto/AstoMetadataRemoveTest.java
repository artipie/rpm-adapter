/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.rpm.RepoConfig;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AstoMetadataRemove}.
 * @since 1.9
 */
class AstoMetadataRemoveTest {

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Test config.
     */
    private RepoConfig config;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.config = new RepoConfig.Simple();
    }

    @Test
    void doesNothingIfStorageIsEmpty() {
        new AstoMetadataRemove(this.storage, this.config).perform(new ListOf<String>("abc123"))
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.storage.list(Key.ROOT).join(),
            Matchers.emptyIterable()
        );
    }

}
