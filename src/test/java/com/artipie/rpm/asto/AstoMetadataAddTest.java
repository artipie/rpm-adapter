/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.rpm.Digest;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.StandardNamingPolicy;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.matchers.XhtmlMatchers;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.zip.GZIPInputStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

/**
 * Test for {@link AstoMetadataAdd}.
 * @since 1.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
class AstoMetadataAddTest {

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
        this.conf = new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.SHA256, false);
    }

    @Test
    void addsEmptyFiles() throws IOException {
        final Key temp = new AstoMetadataAdd(this.storage, this.conf)
            .perform(Collections.emptyList()).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Failed to generate 4 items: metadatas and checksums",
            this.storage.list(Key.ROOT).join(),
            Matchers.iterableWithSize(4)
        );
        MatcherAssert.assertThat(
            "Failed to generate empty primary xml",
            new String(
                this.readAndUnpack(new Key.From(temp, XmlPackage.PRIMARY.name())),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='metadata' and @packages='0']"
            )
        );
        MatcherAssert.assertThat(
            "Failed to generate empty other xml",
            new String(
                this.readAndUnpack(new Key.From(temp, XmlPackage.OTHER.name())),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='otherdata' and @packages='0']"
            )
        );
    }

    private byte[] readAndUnpack(final Key key) throws IOException {
        return IOUtils.toByteArray(
            new GZIPInputStream(
                new ByteArrayInputStream(new BlockingStorage(this.storage).value(key))
            )
        );
    }

}
