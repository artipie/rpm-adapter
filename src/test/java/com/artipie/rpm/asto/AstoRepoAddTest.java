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
import java.util.zip.GZIPInputStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

/**
 * Test for {@link AstoRepoAdd}.
 * @since 1.10
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AstoRepoAddTest {

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
        this.conf = new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.PLAIN, false);
    }

    @Test
    void createsEmptyMetadata() throws IOException {
        new AstoRepoAdd(this.storage, this.conf).perform().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Failed to generate 3 items: primary, filelists and repomd",
            this.storage.list(Key.ROOT).join(),
            Matchers.iterableWithSize(3)
        );
        MatcherAssert.assertThat(
            "Failed to generate empty primary xml",
            new String(
                this.readAndUnpack(XmlPackage.PRIMARY), StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths("/*[local-name()='metadata' and @packages='0']")
        );
        MatcherAssert.assertThat(
            "Failed to generate empty other xml",
            new String(
                this.readAndUnpack(XmlPackage.OTHER), StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths("/*[local-name()='otherdata' and @packages='0']")
        );
        MatcherAssert.assertThat(
            new String(
                new BlockingStorage(this.storage).value(new Key.From("metadata", "repomd.xml")),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths("/*[local-name()='repomd']/*[local-name()='revision']")
        );
    }

    private byte[] readAndUnpack(final XmlPackage type) throws IOException {
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        return IOUtils.toByteArray(
            new GZIPInputStream(
                new ByteArrayInputStream(
                    bsto.value(
                        bsto.list(new Key.From("metadata")).stream()
                            .filter(item -> item.string().contains(type.lowercase()))
                            .findFirst().get()
                    )
                )
            )
        );
    }
}
